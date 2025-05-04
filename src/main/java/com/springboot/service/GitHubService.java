package com.springboot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GitHubService {
    
    @Value("${github.api.token}")
    private String githubToken;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    
    public List<Map<String, Object>> getUserCommits(String username) {
        List<Map<String, Object>> allEvents = new ArrayList<>();
        int perPage = 30;
        int maxPages = 10;
        for (int page = 1; page <= maxPages; page++) {
            String url = UriComponentsBuilder.fromHttpUrl(GITHUB_API_BASE_URL)
                    .path("/users/{username}/events")
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .buildAndExpand(username)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + githubToken);
            headers.set("Accept", "application/vnd.github.v3+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            List<Map<String, Object>> events = response.getBody();
            if (events == null || events.isEmpty()) {
                break;
            }
            allEvents.addAll(events);
            if (events.size() < perPage) {
                break;
            }
        }
        List<Map<String, Object>> events = allEvents;
        if (events != null) {
            // CreateEvent, DeleteEvent 제외
            events.removeIf(event -> {
                String type = (String) event.get("type");
                if ("CreateEvent".equals(type) || "DeleteEvent".equals(type)) {
                    Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                    if (payload != null) {
                        String refType = (String) payload.get("ref_type");
                        // 브랜치 또는 태그 생성/삭제만 제외 (리포지토리 생성은 남김)
                        return "branch".equals(refType) || "tag".equals(refType);
                    }
                }
                return false;
            });
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            ZoneId seoulZone = ZoneId.of("Asia/Seoul");
            for (Map<String, Object> event : events) {
                Object createdAtObj = event.get("created_at");
                if (createdAtObj instanceof String createdAt) {
                    try {
                        ZonedDateTime zdt = ZonedDateTime.parse(createdAt);
                        ZonedDateTime seoulTime = zdt.withZoneSameInstant(seoulZone);
                        String formatted = seoulTime.format(formatter);
                        event.put("created_at_formatted", formatted);
                        event.put("created_at_timezone", "KST");
                        int offsetHours = seoulTime.getOffset().getTotalSeconds() / 3600;
                        String offsetStr = (offsetHours >= 0 ? "+" : "") + offsetHours + ":00";
                        event.put("created_at_offset", offsetStr);
                    } catch (Exception ignored) {}
                }
                if ("PushEvent".equals(event.get("type"))) {
                    Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                    if (payload != null && payload.get("commits") instanceof List) {
                        List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
                        List<String> commitMessages = new ArrayList<>();
                        for (Map<String, Object> commit : commits) {
                            Object msg = commit.get("message");
                            if (msg != null) commitMessages.add(msg.toString());
                        }
                        event.put("commitMessages", commitMessages);
                    }
                }
            }
        }
        return events;
    }
} 