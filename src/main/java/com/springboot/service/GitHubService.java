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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GitHubService 
{

    @Autowired
    private RestTemplate restTemplate;

    @Value("${github.api.token}")
    private String githubToken;

    private static final String GITHUB_API_BASE_URL = "https://api.github.com";

    // Helper method to get user's public repositories
    private List<Map<String, Object>> getUserPublicRepositories(String username, HttpEntity<String> entity) {
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

    // Helper method to get commits for a repository and transform them
    // Only fetch the first page of commits (default up to 100)
    private List<Map<String, Object>> getCommitsForOwnerRepo(String owner, String repoName, String repoFullName, HttpEntity<String> entity) 
    {
        List<Map<String, Object>> transformedCommits = new ArrayList<>();
        
        // Removed pagination loop to fetch only the first page (default 100 commits, can be adjusted with per_page)
        String url = GITHUB_API_BASE_URL + "/repos/" + owner + "/" + repoName + "/commits?per_page=100"; // Fetch up to 100 commits
        
        try 
        {
            ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> rawCommitsThisPage = responseEntity.getBody();
            
            if (rawCommitsThisPage != null && !rawCommitsThisPage.isEmpty())
            {
                for (Map<String, Object> rawCommit : rawCommitsThisPage)
                {
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
                    { // Fallback to committer date
                        Map<String, Object> committerDetails = (Map<String, Object>) commitDetails.get("committer");
                        
                        if (committerDetails != null && committerDetails.get("date") != null)
                            commitDate = (String) committerDetails.get("date");
                    }
                    
                    if (commitDate == null)
                        commitDate = Instant.now().toString(); 

                    commitEvent.put("created_at", commitDate);

                    List<Map<String, Object>> commitListPayload = new ArrayList<>();
                    Map<String, Object> commitDataForPayload = new HashMap<>();
                    
                    commitDataForPayload.put("message", commitMessage);
                    commitDataForPayload.put("sha", commitSha); 
                    commitListPayload.add(commitDataForPayload);

                    Map<String, Object> payload = new HashMap<>();
                    
                    payload.put("commits", commitListPayload);
                    commitEvent.put("payload", payload);
                    
                    transformedCommits.add(commitEvent);
                }
            }
        } 
        
        catch (Exception e) 
        {
            // Log error or handle it - e.g., if a repo is empty or inaccessible
            System.err.println("Error fetching commits for " + repoFullName + ": " + e.getMessage());
        }
        
        return transformedCommits;
    }

    @Cacheable(value = "userCommits", key = "#username")
    public List<Map<String, Object>> getUserCommits(String username)
    {
        List<Map<String, Object>> allCollectedEvents = new ArrayList<>();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("Authorization", "token " + githubToken);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 1. Fetch direct commits from all public repositories
        List<Map<String, Object>> publicRepos = getUserPublicRepositories(username, entity);
        
        for (Map<String, Object> repoData : publicRepos) 
        {
            String repoShortName = (String) repoData.get("name");
            String repoFullName = (String) repoData.get("full_name"); 
            
            // Owner login might be nested if using /users/{username}/repos
            Map<String, Object> ownerMap = (Map<String, Object>) repoData.get("owner");
            String ownerLogin = (String) ownerMap.get("login");

            if (repoShortName != null && ownerLogin != null && repoFullName != null)
                 allCollectedEvents.addAll(getCommitsForOwnerRepo(ownerLogin, repoShortName, repoFullName, entity));
        }

        // 2. Fetch other event types (non-PushEvent) from user events endpoint
        int page = 1;
        boolean hasMorePages = true;
        
        while (hasMorePages) 
        {
            String url = GITHUB_API_BASE_URL + "/users/" + username + "/events?per_page=100&page=" + page;
            
            ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
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

        // 3. Sort all collected events by date (descending)
        allCollectedEvents.sort(Comparator.comparing((Map<String, Object> event) -> 
            Instant.parse((String) event.get("created_at"))
        ).reversed());


        // 4. Post-processing (date formatting, colors, etc.)
        if (!allCollectedEvents.isEmpty()) 
        {
            String[] colors = {"#facc15", "#22d3ee", "#34d399", "#60a5fa", "#f87171"};
            
            for (int i = 0; i < allCollectedEvents.size(); i++) 
            {
                Map<String, Object> event = allCollectedEvents.get(i);

                // 날짜 포맷팅
                if (event.containsKey("created_at")) 
                {
                    String createdAt = (String) event.get("created_at");
                    Instant instant = Instant.parse(createdAt);
                    ZoneId zoneId = ZoneId.of("Asia/Seoul");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm")
                        .withZone(zoneId);
                    
                    event.put("created_at_formatted", formatter.format(instant));
                    event.put("created_at_timezone", "KST");
                    event.put("created_at_offset", "+09:00");
                }

                // 색상 할당
                event.put("color", colors[i % colors.length]);

                // PushEvent인 경우 커밋 메시지 추출
                if ("PushEvent".equals(event.get("type"))) 
                {
                    Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                    if (payload != null && payload.containsKey("commits")) 
                    {
                        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
                        List<String> commitMessages = new ArrayList<>();
                        for (Map<String, Object> commit : commits) {
                            commitMessages.add((String) commit.get("message"));
                        }
                        event.put("commitMessages", commitMessages);
                    }
                }
            }
        }
        return allCollectedEvents;
    }
} 