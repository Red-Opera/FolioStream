package com.springboot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import jakarta.annotation.PostConstruct;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

@Service
public class GitHubService
{
    private final RestTemplate restTemplate; 	// 기존 RestTemplate 유지 또는 점진적 제거
    private final WebClient webClient;			// WebClient 추가
    private final String githubToken;
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final int CACHE_DURATION_MINUTES = 30; // 캐시 유효 시간 30분
    private static final int MAX_CONCURRENT_REQUESTS = 10; // 동시 요청 수 제한
    private static final int BATCH_SIZE = 5; // 배치 처리 크기
    private static final int MAX_EVENT_PAGES = 10; // 최대 이벤트 페이지 수

    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Math.min(Runtime.getRuntime().availableProcessors() * 2, 10)
    );

    // 캐시를 위한 Map 추가
    private final Map<String, CacheEntry> userCommitsCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> userReposCache = new ConcurrentHashMap<>();

    // 기본 사용자 목록 (앱 시작 시 캐시할 사용자들)
    private static final List<String> DEFAULT_USERS = List.of("Red-Opera");

    // 자주 사용되는 사용자 목록을 저장할 Set
    private final Set<String> activeUsers = Collections.synchronizedSet(new HashSet<>());

    // 캐시 엔트리 클래스
    private static class CacheEntry {
        private final List<Map<String, Object>> data;
        private final Instant expiryTime;

        public CacheEntry(List<Map<String, Object>> data, int durationMinutes) {
            this.data = data;
            this.expiryTime = Instant.now().plus(durationMinutes, ChronoUnit.MINUTES);
        }

        public boolean isValid() {
            return Instant.now().isBefore(expiryTime);
        }
    }

    @Autowired
    public GitHubService(RestTemplate restTemplate, WebClient.Builder webClientBuilder, @Value("${github.api.token}") String githubToken) 
    {
        this.restTemplate = restTemplate;
        this.githubToken = githubToken;
        
        final int bufferSize = 16 * 1024 * 1024;
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize))
                .build();

        this.webClient = webClientBuilder
                            .baseUrl(GITHUB_API_BASE_URL)
                            .exchangeStrategies(exchangeStrategies)
                            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                            .defaultHeader(HttpHeaders.AUTHORIZATION, "token " + this.githubToken)
                            .build();
    }

    // Helper method to get user's public repositories with rate limit handling
    private List<Map<String, Object>> getUserPublicRepositories(String username, HttpEntity<String> entity)
    {
        // 캐시 확인
        CacheEntry cacheEntry = userReposCache.get(username);
        if (cacheEntry != null && cacheEntry.isValid()) {
            return cacheEntry.data;
        }

        List<Map<String, Object>> allRepositories = new ArrayList<>();
        
        int page = 1;
        boolean hasMorePages = true;
        int retryCount = 0;
        int maxRetries = 3;
        long retryDelayMs = 5000;

        while (hasMorePages && retryCount <= maxRetries) {
            try {
                String url = GITHUB_API_BASE_URL + "/users/" + username + "/repos?type=public&per_page=100&page=" + page;
                
                ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                
                HttpHeaders headers = responseEntity.getHeaders();
                logRateLimitInfo(headers);

                List<Map<String, Object>> reposThisPage = responseEntity.getBody();
                
                if (reposThisPage != null && !reposThisPage.isEmpty()) {
                    allRepositories.addAll(reposThisPage);
                    
                    if (reposThisPage.size() < 100) 
                        hasMorePages = false;
                    else 
                        page++;
                } else {
                    hasMorePages = false;
                }
                
                retryCount = 0;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.FORBIDDEN && 
                    e.getResponseBodyAsString().contains("API rate limit exceeded")) {
                    
                    retryCount++;
                    if (retryCount > maxRetries) {
                        System.err.println("최대 재시도 횟수를 초과했습니다. GitHub API 레이트 리밋 한도에 도달했습니다.");
                        throw new RuntimeException("GitHub API 레이트 리밋 한도에 도달했습니다. 나중에 다시 시도해주세요.", e);
                    }
                    
                    long waitTime = retryDelayMs * (long)Math.pow(2, retryCount - 1);
                    System.out.println("GitHub API 레이트 리밋 한도에 도달했습니다. " + (waitTime / 1000) + "초 후 재시도합니다...");
                    
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("레이트 리밋 대기 중 인터럽트가 발생했습니다.", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        
        // 결과를 캐시에 저장
        userReposCache.put(username, new CacheEntry(allRepositories, CACHE_DURATION_MINUTES));
        
        return allRepositories;
    }

    // 레이트 리밋 정보 로깅 메소드 추가
    private void logRateLimitInfo(HttpHeaders headers) {
        String rateLimit = headers.getFirst("X-RateLimit-Limit");
        String rateRemaining = headers.getFirst("X-RateLimit-Remaining");
        String rateReset = headers.getFirst("X-RateLimit-Reset");
        
        if (rateLimit != null && rateRemaining != null && rateReset != null) {
            long resetTime = Long.parseLong(rateReset);
            Instant resetInstant = Instant.ofEpochSecond(resetTime);
            String resetTimeFormatted = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(resetInstant);
            
            System.out.println("GitHub API 레이트 리밋 정보: " + 
                              "한도=" + rateLimit + 
                              ", 남은 요청=" + rateRemaining + 
                              ", 리셋 시간=" + resetTimeFormatted);
            
            // 레이트 리밋 임계치 도달 시 경고 (예: 10% 이하로 남았을 경우)
            if (rateRemaining != null && Integer.parseInt(rateRemaining) < Integer.parseInt(rateLimit) * 0.1) {
                System.out.println("경고: GitHub API 레이트 리밋이 거의 소진되었습니다!");
            }
        }
    }

    // WebClient를 사용하여 저장소의 커밋을 가져오고 변환하는 Helper 메소드 개선
    private Flux<Map<String, Object>> getCommitsForOwnerRepoReactive(String owner, String repoName, String repoFullName) 
    {
        String path = "/repos/" + owner + "/" + repoName + "/commits";

        return this.webClient.get()
            .uri(uriBuilder -> uriBuilder.path(path)
                .queryParam("per_page", 100)
                .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
            .flatMapMany(Flux::fromIterable)
            .flatMap(rawCommit -> {
                String commitSha = (String) rawCommit.get("sha");
                
                // 각 커밋에 대해 상세 정보를 추가로 가져옵니다
                return this.webClient.get()
                    .uri("/repos/" + owner + "/" + repoName + "/commits/" + commitSha)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .map(commitDetail -> {
                        Map<String, Object> commitEvent = new HashMap<>();
                        commitEvent.put("type", "PushEvent");
                        
                        Map<String, Object> repoInfo = new HashMap<>();
                        repoInfo.put("name", repoFullName);
                        commitEvent.put("repo", repoInfo);

                        Map<String, Object> commitDetails = (Map<String, Object>) rawCommit.get("commit");
                        String commitMessage = "(No commit message)";
                        
                        if (commitDetails != null && commitDetails.get("message") != null)
                            commitMessage = (String) commitDetails.get("message");
                        
                        Map<String, Object> authorDetails = (Map<String, Object>) commitDetails.get("author");
                        String commitDate = null;
                        
                        if (authorDetails != null && authorDetails.get("date") != null)
                            commitDate = (String) authorDetails.get("date");
                        else if (commitDetails != null) 
                        {
                            Map<String, Object> committerDetails = (Map<String, Object>) commitDetails.get("committer");
                            if (committerDetails != null && committerDetails.get("date") != null)
                                commitDate = (String) committerDetails.get("date");
                        }
                        
                        if (commitDate == null) 
                            commitDate = Instant.now().toString();
                        
                        commitEvent.put("created_at", commitDate);

                        // 변경된 라인 수 정보 가져오기
                        Map<String, Object> stats = (Map<String, Object>) commitDetail.get("stats");
                        if (stats != null) {
                            Integer additions = (Integer) stats.get("additions");
                            Integer deletions = (Integer) stats.get("deletions");
                            
                            commitEvent.put("additions", additions);
                            commitEvent.put("deletions", deletions);
                        }
                        
                        List<Map<String, Object>> commitListPayload = new ArrayList<>();
                        Map<String, Object> commitDataForPayload = new HashMap<>();
                        
                        commitDataForPayload.put("message", commitMessage);
                        commitDataForPayload.put("sha", commitSha);
                        commitListPayload.add(commitDataForPayload);

                        Map<String, Object> payload = new HashMap<>();
                        payload.put("commits", commitListPayload);
                        commitEvent.put("payload", payload);
                        
                        return commitEvent;
                    });
            }, MAX_CONCURRENT_REQUESTS)
            .onErrorResume(e -> {
                if (e instanceof WebClientResponseException) {
                    WebClientResponseException wcre = (WebClientResponseException) e;
                    if (wcre.getStatusCode() == HttpStatus.FORBIDDEN && 
                        wcre.getResponseBodyAsString().contains("API rate limit exceeded")) {
                        System.err.println("GitHub API 레이트 리밋 초과: " + repoFullName);
                        return Flux.error(new RuntimeException("GitHub API 레이트 리밋 초과"));
                    }
                }
                System.err.println("Error fetching commits for " + repoFullName + ": " + e.getMessage());
                return Flux.empty();
            })
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                .filter(ex -> ex instanceof RuntimeException && 
                              ex.getMessage().contains("GitHub API 레이트 리밋 초과")));
    }

    /**
     * 앱 시작 시 초기 캐시 로드
     */
    @PostConstruct
    public void initializeCache() {
        System.out.println("Initializing GitHub data cache...");
        
        // 기본 사용자들의 데이터를 미리 캐시
        for (String username : DEFAULT_USERS) {
            try {
                System.out.println("Pre-loading data for default user: " + username);
                CompletableFuture.runAsync(() -> {
                    try {
                        getUserCommits(username);
                        System.out.println("Successfully pre-loaded data for user: " + username);
                    } catch (Exception e) {
                        System.err.println("Error pre-loading data for user " + username + ": " + e.getMessage());
                    }
                }, executorService);
                
                // 레이트 리밋을 고려하여 요청 간 약간의 지연 추가
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 사용자 추가 메소드 - 새로운 사용자가 조회될 때 호출됨
     */
    public void addActiveUser(String username) {
        activeUsers.add(username);
    }

    /**
     * 30분마다 실행되는 스케줄된 작업
     * 활성 사용자들의 데이터를 미리 가져와 캐시에 저장
     */
    @Scheduled(fixedRate = 1800000) // 30분 = 1800000ms
    public void scheduledDataFetch() {
        System.out.println("Starting scheduled data fetch for active users...");
        
        // 활성 사용자 목록을 복사하여 사용 (동시성 문제 방지)
        Set<String> usersToUpdate = new HashSet<>(activeUsers);
        
        for (String username : usersToUpdate) {
            try {
                System.out.println("Pre-fetching data for user: " + username);
                // 비동기로 데이터 가져오기
                CompletableFuture.runAsync(() -> {
                    try {
                        getUserCommits(username);
                        System.out.println("Successfully pre-fetched data for user " + username);
                    } catch (Exception e) {
                        System.err.println("Error pre-fetching data for user " + username + ": " + e.getMessage());
                    }
                }, executorService);
                
                // 레이트 리밋을 고려하여 요청 간 약간의 지연 추가
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Cacheable(value = "userCommits", key = "#username")
    public List<Map<String, Object>> getUserCommits(String username)
    {
        // 활성 사용자 목록에 추가
        addActiveUser(username);

        // 캐시 확인
        CacheEntry cacheEntry = userCommitsCache.get(username);
        if (cacheEntry != null && cacheEntry.isValid()) {
            return cacheEntry.data;
        }

        List<Map<String, Object>> allCollectedEvents = new ArrayList<>();
        
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", "application/vnd.github.v3+json");
        httpHeaders.set("Authorization", "token " + githubToken);
        
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);

        try {
            // 1. 공개 저장소 목록 가져오기
            List<Map<String, Object>> publicRepos = getUserPublicRepositories(username, entity);

            // 2. 저장소를 배치로 나누어 병렬 처리
            List<List<Map<String, Object>>> repoBatches = new ArrayList<>();
            for (int i = 0; i < publicRepos.size(); i += BATCH_SIZE) {
                repoBatches.add(publicRepos.subList(i, Math.min(i + BATCH_SIZE, publicRepos.size())));
            }

            // 3. 각 배치를 병렬로 처리
            for (List<Map<String, Object>> repoBatch : repoBatches) {
                List<Map<String, Object>> batchCommits = Flux.fromIterable(repoBatch)
                    .flatMap(repoData -> {
                        String repoShortName = (String) repoData.get("name");
                        String repoFullName = (String) repoData.get("full_name");
                        Map<String, Object> ownerMap = (Map<String, Object>) repoData.get("owner");
                        String ownerLogin = (String) ownerMap.get("login");

                        if (repoShortName != null && ownerLogin != null && repoFullName != null)
                            return getCommitsForOwnerRepoReactive(ownerLogin, repoShortName, repoFullName);
                        
                        return Flux.empty();
                    }, MAX_CONCURRENT_REQUESTS)
                    .collectList()
                    .block();

                if (batchCommits != null)
                    allCollectedEvents.addAll(batchCommits);

                // 배치 간 짧은 대기 시간 추가
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 4. 사용자 이벤트 가져오기 (병렬 처리)
            List<Map<String, Object>> userEvents = Flux.range(1, MAX_EVENT_PAGES)
                .flatMap(page -> 
                    this.webClient.get()
                        .uri(uriBuilder -> uriBuilder
                            .path("/users/{username}/events")
                            .queryParam("per_page", 100)
                            .queryParam("page", page)
                            .build(username))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                        .flatMapMany(Flux::fromIterable)
                        .filter(event -> !"PushEvent".equals(event.get("type")))
                        .onErrorResume(e -> Flux.empty()),
                    MAX_CONCURRENT_REQUESTS
                )
                .collectList()
                .block();

            if (userEvents != null)
                allCollectedEvents.addAll(userEvents);

            // 5. 이벤트 정렬 및 후처리
            if (!allCollectedEvents.isEmpty()) {
                allCollectedEvents.sort(Comparator.comparing((Map<String, Object> event) ->
                    Instant.parse((String) event.get("created_at"))
                ).reversed());

                // 날짜 포맷팅 및 색상 적용
                String[] colors = {"#facc15", "#22d3ee", "#34d399", "#60a5fa", "#f87171"};
                
                for (int i = 0; i < allCollectedEvents.size(); i++) {
                    Map<String, Object> event = allCollectedEvents.get(i);
                    
                    if (event.containsKey("created_at")) {
                        String createdAt = (String) event.get("created_at");
                        Instant instant = Instant.parse(createdAt);
                        ZoneId zoneId = ZoneId.of("Asia/Seoul");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm").withZone(zoneId);
                        
                        event.put("created_at_formatted", formatter.format(instant));
                        event.put("created_at_timezone", "KST");
                        event.put("created_at_offset", "+09:00");
                    }
                    
                    event.put("color", colors[i % colors.length]);
                    
                    if ("PushEvent".equals(event.get("type"))) {
                        Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                        
                        if (payload != null && payload.containsKey("commits")) {
                            List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
                            List<String> commitMessages = new ArrayList<>();
                            
                            for (Map<String, Object> commit : commits)
                                commitMessages.add((String) commit.get("message"));

                            event.put("commitMessages", commitMessages);
                            
                            if (event.containsKey("additions") && event.containsKey("deletions")) {
                                Integer additions = (Integer) event.get("additions");
                                Integer deletions = (Integer) event.get("deletions");
                                event.put("changes_summary", additions + "줄 추가, " + deletions + "줄 제거");
                            }
                        }
                    }
                }
            }

            // 결과를 캐시에 저장
            userCommitsCache.put(username, new CacheEntry(allCollectedEvents, CACHE_DURATION_MINUTES));
            
        } catch (Exception e) {
            Map<String, Object> errorEvent = new HashMap<>();
            errorEvent.put("type", "ErrorEvent");
            errorEvent.put("created_at", Instant.now().toString());
            
            Instant instant = Instant.now();
            ZoneId zoneId = ZoneId.of("Asia/Seoul");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm").withZone(zoneId);
            errorEvent.put("created_at_formatted", formatter.format(instant));
            errorEvent.put("created_at_timezone", "KST");
            errorEvent.put("created_at_offset", "+09:00");
            
            Map<String, Object> repoInfo = new HashMap<>();
            repoInfo.put("name", "GitHub API 오류");
            errorEvent.put("repo", repoInfo);
            
            Map<String, Object> payload = new HashMap<>();
            List<Map<String, Object>> errorDetails = new ArrayList<>();
            Map<String, Object> errorDetail = new HashMap<>();
            
            if (e.getMessage().contains("rate limit exceeded")) {
                errorDetail.put("message", "GitHub API 레이트 리밋 한도에 도달했습니다. 잠시 후 다시 시도해주세요.");
            } else {
                errorDetail.put("message", "GitHub 데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            }
            
            errorDetail.put("sha", "error-" + System.currentTimeMillis());
            errorDetails.add(errorDetail);
            payload.put("commits", errorDetails);
            errorEvent.put("payload", payload);
            
            errorEvent.put("color", "#ef4444");
            
            allCollectedEvents.add(errorEvent);
        }
        
        return allCollectedEvents;
    }

    @PreDestroy
    public void shutdownExecutor() 
    {
        if (executorService != null && !executorService.isShutdown()) 
        {
            System.out.println("Shutting down ExecutorService in GitHubService");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}