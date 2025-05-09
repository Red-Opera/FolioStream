package com.springboot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PreDestroy;
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

    // Helper method to get user's public repositories (기존 RestTemplate 사용 또는 WebClient로 변경 필요)
    private List<Map<String, Object>> getUserPublicRepositories(String username, HttpEntity<String> entity)
    {
    	// GitHub API에서 사용자의 공개 저장소를 가져오는 메소드
        List<Map<String, Object>> allRepositories = new ArrayList<>();
        
        int page = 1;
        boolean hasMorePages = true;

        while (hasMorePages)
        {
            String url = GITHUB_API_BASE_URL + "/users/" + username + "/repos?type=public&per_page=100&page=" + page;
            
            ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange
            (
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> reposThisPage = responseEntity.getBody();
            
            if (reposThisPage != null && !reposThisPage.isEmpty())
            {
                allRepositories.addAll(reposThisPage);
                
                if (reposThisPage.size() < 100) 
                	hasMorePages = false;
                
                else 
                	page++;
            }
            
            else 
            	hasMorePages = false;
        }
        
        return allRepositories;
    }

    // WebClient를 사용하여 저장소의 커밋을 가져오고 변환하는 Helper 메소드
    private Flux<Map<String, Object>> getCommitsForOwnerRepoReactive(String owner, String repoName, String repoFullName) 
    {
        String path = "/repos/" + owner + "/" + repoName + "/commits";

        return this.webClient.get()
            .uri(uriBuilder -> uriBuilder.path(path).queryParam("per_page", 100).build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {}) // API는 커밋 객체의 리스트를 반환
            .flatMapMany(Flux::fromIterable) // 리스트를 개별 커밋 객체의 Flux로 변환
            .map(rawCommit -> 
            { // 각 raw 커밋 변환
                Map<String, Object> commitEvent = new HashMap<>();
                commitEvent.put("type", "PushEvent");
                
                Map<String, Object> repoInfo = new HashMap<>();
                repoInfo.put("name", repoFullName);
                commitEvent.put("repo", repoInfo);

                Map<String, Object> commitDetails = (Map<String, Object>) rawCommit.get("commit");
                String commitMessage = "(No commit message)";
                
                if (commitDetails != null && commitDetails.get("message") != null)
                    commitMessage = (String) commitDetails.get("message");
                
                String commitSha = (String) rawCommit.get("sha");

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
                    commitDate = Instant.now().toString(); // Fallback
                
                commitEvent.put("created_at", commitDate);

                List<Map<String, Object>> commitListPayload = new ArrayList<>();
                Map<String, Object> commitDataForPayload = new HashMap<>();
                
                commitDataForPayload.put("message", commitMessage);
                commitDataForPayload.put("sha", commitSha);
                commitListPayload.add(commitDataForPayload);

                Map<String, Object> payload = new HashMap<>();
                payload.put("commits", commitListPayload);
                commitEvent.put("payload", payload);
                
                return commitEvent;
            })
            .onErrorResume(e -> 
            {
                System.err.println("Error fetching commits for " + repoFullName + " reactively: " + e.getMessage());
                return Flux.empty(); // 실패 시 다른 저장소 처리는 계속
            });
    }

    @Cacheable(value = "userCommits", key = "#username")
    public List<Map<String, Object>> getUserCommits(String username)
    {
        List<Map<String, Object>> allCollectedEvents = new ArrayList<>();
        
        HttpHeaders httpHeaders = new HttpHeaders(); // getUserPublicRepositories 또는 /events API 호출 시 필요할 수 있음
        httpHeaders.set("Accept", "application/vnd.github.v3+json");
        httpHeaders.set("Authorization", "token " + githubToken);
        
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);

        // 1. 공개 저장소에서 직접 커밋 가져오기 (WebClient와 Reactive Streams 사용)
        // getUserPublicRepositories가 WebClient를 사용하도록 수정되었다고 가정하거나,
        // 기존 동기 방식을 유지하고 결과를 Flux로 변환합니다.
        List<Map<String, Object>> publicRepos = getUserPublicRepositories(username, entity); 

        List<Map<String, Object>> directCommits = Flux.fromIterable(publicRepos)
            // .publishOn(Schedulers.fromExecutorService(executorService)) // 병렬 처리를 위해 스케줄러 사용 가능
            .flatMap(repoData -> 
            {
                String repoShortName = (String) repoData.get("name");
                String repoFullName = (String) repoData.get("full_name");
                
                Map<String, Object> ownerMap = (Map<String, Object>) repoData.get("owner");
                String ownerLogin = (String) ownerMap.get("login");

                if (repoShortName != null && ownerLogin != null && repoFullName != null)
                    return getCommitsForOwnerRepoReactive(ownerLogin, repoShortName, repoFullName);
                
                return Flux.empty();
            }, Math.min(publicRepos.size(), 10)) // flatMap의 동시성 제어 (예: 최대 10개 동시 실행)
            .collectList()
            .block(); // 결과를 동기적으로 기다림 (전체 파이프라인이 리액티브하면 .block() 없이 처리)

        if (directCommits != null)
            allCollectedEvents.addAll(directCommits);

        // 2. 사용자 이벤트 엔드포인트에서 다른 이벤트 유형(PushEvent 제외) 가져오기
        // 이 부분도 WebClient를 사용하도록 수정하는 것이 좋습니다.
        // 설명을 위해 기존 RestTemplate 코드를 유지합니다.
        int page = 1;
        boolean hasMorePages = true;
        
        while (hasMorePages)
        {
            String url = GITHUB_API_BASE_URL + "/users/" + username + "/events?per_page=100&page=" + page;
            ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> eventsThisPage = responseEntity.getBody();
            
            if (eventsThisPage != null && !eventsThisPage.isEmpty())
            {
                for (Map<String, Object> event : eventsThisPage)
                {
                    if (!"PushEvent".equals(event.get("type")))
                        allCollectedEvents.add(event);
                }
                
                if (eventsThisPage.size() < 100) 
                	hasMorePages = false;
                
                else 
                	page++;
            }
            
            else 
            	hasMorePages = false;
        }

        // 3. 수집된 모든 이벤트를 날짜순으로 정렬 (내림차순)
        allCollectedEvents.sort(Comparator.comparing((Map<String, Object> event) ->
            Instant.parse((String) event.get("created_at"))
        ).reversed());

        // 4. 후처리 (날짜 포맷팅, 색상 등)
        if (!allCollectedEvents.isEmpty())
        {
            String[] colors = {"#facc15", "#22d3ee", "#34d399", "#60a5fa", "#f87171"};
            
            for (int i = 0; i < allCollectedEvents.size(); i++)
            {
                Map<String, Object> event = allCollectedEvents.get(i);
                
                if (event.containsKey("created_at"))
                {
                    String createdAt = (String) event.get("created_at");
                    Instant instant = Instant.parse(createdAt);
                    ZoneId zoneId = ZoneId.of("Asia/Seoul");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm").withZone(zoneId);
                    
                    event.put("created_at_formatted", formatter.format(instant));
                    event.put("created_at_timezone", "KST");
                    event.put("created_at_offset", "+09:00");
                }
                
                event.put("color", colors[i % colors.length]);
                
                if ("PushEvent".equals(event.get("type")))
                {
                    Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                    
                    if (payload != null && payload.containsKey("commits"))
                    {
                        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
                        List<String> commitMessages = new ArrayList<>();
                        
                        for (Map<String, Object> commit : commits)
                            commitMessages.add((String) commit.get("message"));

                        event.put("commitMessages", commitMessages);
                    }
                }
            }
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