package com.springboot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

@Service
public class GitHubService
{
    private final RestTemplate restTemplate; 	// 기존 RestTemplate 유지 또는 점진적 제거
    private final WebClient webClient;			// WebClient 추가
    private final String githubToken;
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";

    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Math.min(Runtime.getRuntime().availableProcessors() * 2, 10)
    );

    @Autowired
    public GitHubService(RestTemplate restTemplate, WebClient.Builder webClientBuilder, @Value("${github.api.token}") String githubToken) 
    {
        this.restTemplate = restTemplate;
        this.githubToken = githubToken;
        
        final int bufferSize = 16 * 1024 * 1024; // 16MB로 설정 (필요에 따라 조절)
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSize))
                .build();

        this.webClient = webClientBuilder
                            .baseUrl(GITHUB_API_BASE_URL)
                            .exchangeStrategies(exchangeStrategies) // 설정된 ExchangeStrategies 적용
                            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                            .defaultHeader(HttpHeaders.AUTHORIZATION, "token " + this.githubToken)
                            .build();
    }

    // Helper method to get user's public repositories with rate limit handling
    private List<Map<String, Object>> getUserPublicRepositories(String username, HttpEntity<String> entity)
    {
        // GitHub API에서 사용자의 공개 저장소를 가져오는 메소드
        List<Map<String, Object>> allRepositories = new ArrayList<>();
        
        int page = 1;
        boolean hasMorePages = true;
        int retryCount = 0;
        int maxRetries = 3;
        long retryDelayMs = 5000; // 초기 재시도 지연 시간 (5초)

        while (hasMorePages && retryCount <= maxRetries) {
            try {
                String url = GITHUB_API_BASE_URL + "/users/" + username + "/repos?type=public&per_page=100&page=" + page;
                
                ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                
                // 헤더에서 레이트 리밋 정보 추출
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
                
                // 성공했으므로 재시도 카운터 초기화
                retryCount = 0;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.FORBIDDEN && 
                    e.getResponseBodyAsString().contains("API rate limit exceeded")) {
                    
                    retryCount++;
                    if (retryCount > maxRetries) {
                        System.err.println("최대 재시도 횟수를 초과했습니다. GitHub API 레이트 리밋 한도에 도달했습니다.");
                        throw new RuntimeException("GitHub API 레이트 리밋 한도에 도달했습니다. 나중에 다시 시도해주세요.", e);
                    }
                    
                    // 지수 백오프 지연 구현
                    long waitTime = retryDelayMs * (long)Math.pow(2, retryCount - 1);
                    System.out.println("GitHub API 레이트 리밋 한도에 도달했습니다. " + (waitTime / 1000) + "초 후 재시도합니다...");
                    
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("레이트 리밋 대기 중 인터럽트가 발생했습니다.", ie);
                    }
                } else {
                    // 다른 종류의 오류는 그대로 전파
                    throw e;
                }
            }
        }
        
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

    // WebClient를 사용하여 저장소의 커밋을 가져오고 변환하는 Helper 메소드 (레이트 리밋 처리 추가)
    private Flux<Map<String, Object>> getCommitsForOwnerRepoReactive(String owner, String repoName, String repoFullName) 
    {
        String path = "/repos/" + owner + "/" + repoName + "/commits";

        return this.webClient.get()
            .uri(uriBuilder -> uriBuilder.path(path).queryParam("per_page", 100).build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {}) // API는 커밋 객체의 리스트를 반환
            .flatMapMany(Flux::fromIterable) // 리스트를 개별 커밋 객체의 Flux로 변환
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
                            
                            // 변경 라인 정보 저장
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
            }, Math.min(10, Runtime.getRuntime().availableProcessors()))
            .onErrorResume(e -> {
                if (e instanceof WebClientResponseException) {
                    WebClientResponseException wcre = (WebClientResponseException) e;
                    if (wcre.getStatusCode() == HttpStatus.FORBIDDEN && 
                        wcre.getResponseBodyAsString().contains("API rate limit exceeded")) {
                        System.err.println("GitHub API 레이트 리밋 초과: " + repoFullName + ", 나중에 재시도합니다.");
                        // 레이트 리밋 초과 시 재시도 로직 추가
                        return Flux.error(new RuntimeException("GitHub API 레이트 리밋 초과"));
                    }
                }
                System.err.println("Error fetching commits for " + repoFullName + " reactively: " + e.getMessage());
                return Flux.empty(); // 실패 시 다른 저장소 처리는 계속
            })
            // 레이트 리밋 초과 시 지수 백오프로 재시도
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                .filter(ex -> ex instanceof RuntimeException && 
                              ex.getMessage().contains("GitHub API 레이트 리밋 초과"))
                .doBeforeRetry(retrySignal -> 
                    System.out.println("레이트 리밋으로 인해 재시도: " + retrySignal.totalRetries() + " - " + repoFullName)));
    }

    @Cacheable(value = "userCommits", key = "#username")
    public List<Map<String, Object>> getUserCommits(String username)
    {
        List<Map<String, Object>> allCollectedEvents = new ArrayList<>();
        
        HttpHeaders httpHeaders = new HttpHeaders(); // getUserPublicRepositories 또는 /events API 호출 시 필요할 수 있음
        httpHeaders.set("Accept", "application/vnd.github.v3+json");
        httpHeaders.set("Authorization", "token " + githubToken);
        
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);

        try {
            // 1. 공개 저장소에서 직접 커밋 가져오기 (WebClient와 Reactive Streams 사용)
            List<Map<String, Object>> publicRepos = getUserPublicRepositories(username, entity); 

            List<Map<String, Object>> directCommits = Flux.fromIterable(publicRepos)
                .flatMap(repoData -> 
                {
                    String repoShortName = (String) repoData.get("name");
                    String repoFullName = (String) repoData.get("full_name");
                    
                    Map<String, Object> ownerMap = (Map<String, Object>) repoData.get("owner");
                    String ownerLogin = (String) ownerMap.get("login");

                    if (repoShortName != null && ownerLogin != null && repoFullName != null)
                        return getCommitsForOwnerRepoReactive(ownerLogin, repoShortName, repoFullName);
                    
                    return Flux.empty();
                }, Math.min(publicRepos.size(), 5)) // 과도한 동시 요청을 피하기 위해 5로 제한
                .collectList()
                .block(); // 결과를 동기적으로 기다림 (전체 파이프라인이 리액티브하면 .block() 없이 처리)

            if (directCommits != null)
                allCollectedEvents.addAll(directCommits);

            // 2. 사용자 이벤트 엔드포인트에서 다른 이벤트 유형(PushEvent 제외) 가져오기
            int page = 1;
            boolean hasMorePages = true;
            int retryCount = 0;
            int maxRetries = 3;
            long retryDelayMs = 5000; // 초기 재시도 지연 시간 (5초)
            
            while (hasMorePages && retryCount <= maxRetries) {
                try {
                    String url = GITHUB_API_BASE_URL + "/users/" + username + "/events?per_page=100&page=" + page;
                    ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                        url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                    );
                    
                    // 레이트 리밋 정보 로깅
                    logRateLimitInfo(responseEntity.getHeaders());
                    
                    List<Map<String, Object>> eventsThisPage = responseEntity.getBody();
                    
                    if (eventsThisPage != null && !eventsThisPage.isEmpty()) {
                        for (Map<String, Object> event : eventsThisPage) {
                            if (!"PushEvent".equals(event.get("type")))
                                allCollectedEvents.add(event);
                        }
                        
                        if (eventsThisPage.size() < 100) 
                            hasMorePages = false;
                        else 
                            page++;
                    } else {
                        hasMorePages = false;
                    }
                    
                    // 성공했으므로 재시도 카운터 초기화
                    retryCount = 0;
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.FORBIDDEN && 
                        e.getResponseBodyAsString().contains("API rate limit exceeded")) {
                        
                        retryCount++;
                        if (retryCount > maxRetries) {
                            System.err.println("최대 재시도 횟수를 초과했습니다. GitHub API 레이트 리밋 한도에 도달했습니다.");
                            break; // 일부 이벤트만 가져오더라도 표시하기 위해 종료
                        }
                        
                        // 지수 백오프 지연 구현
                        long waitTime = retryDelayMs * (long)Math.pow(2, retryCount - 1);
                        System.out.println("GitHub API 레이트 리밋 한도에 도달했습니다. " + (waitTime / 1000) + "초 후 재시도합니다...");
                        
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        // 다른 종류의 오류는 로깅 후 이벤트 수집 중단
                        System.err.println("GitHub 이벤트 조회 중 오류 발생: " + e.getMessage());
                        break;
                    }
                }
            }

            // 3. 수집된 모든 이벤트를 날짜순으로 정렬 (내림차순)
            if (!allCollectedEvents.isEmpty()) {
                allCollectedEvents.sort(Comparator.comparing((Map<String, Object> event) ->
                    Instant.parse((String) event.get("created_at"))
                ).reversed());

                // 4. 후처리 (날짜 포맷팅, 색상 등)
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
                            
                            // 변경 라인 수 정보가 있으면 포맷팅하여 추가
                            if (event.containsKey("additions") && event.containsKey("deletions")) {
                                Integer additions = (Integer) event.get("additions");
                                Integer deletions = (Integer) event.get("deletions");
                                event.put("changes_summary", additions + "줄 추가, " + deletions + "줄 제거");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 모든 예외 상황에서 유저에게 알려줄 메시지 추가
            Map<String, Object> errorEvent = new HashMap<>();
            errorEvent.put("type", "ErrorEvent");
            errorEvent.put("created_at", Instant.now().toString());
            
            // 날짜 포맷팅
            Instant instant = Instant.now();
            ZoneId zoneId = ZoneId.of("Asia/Seoul");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm").withZone(zoneId);
            errorEvent.put("created_at_formatted", formatter.format(instant));
            errorEvent.put("created_at_timezone", "KST");
            errorEvent.put("created_at_offset", "+09:00");
            
            // 에러 정보
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
            
            // 에러 이벤트 색상
            errorEvent.put("color", "#ef4444");
            
            // 에러 이벤트 추가
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
        }
    }
}