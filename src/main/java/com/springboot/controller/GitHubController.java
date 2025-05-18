package com.springboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.springboot.service.GitHubService;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.lang.Exception;

@Controller
public class GitHubController 
{
    @Autowired
    private GitHubService githubService;

    private static final String GITHUB_DATA_SESSION_KEY = "github_data";
    private static final String GITHUB_USERNAME_SESSION_KEY = "github_username";

    @GetMapping("/github/commits")
    public String showLoadingPage(@RequestParam(name = "username", defaultValue = "Red-Opera") String username, Model model)
    {
        model.addAttribute("username", username);
        return "redirect:/github/commits/view?username=" + username;  // 로딩 페이지 대신 commits/view로 리다이렉트
    }

    @GetMapping("/github/commits/view")
    public String showMainPage(@RequestParam(name = "username", defaultValue = "Red-Opera") String username, 
                             Model model, 
                             HttpSession session) 
    {
        // 세션에서 데이터 가져오기
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) session.getAttribute(GITHUB_DATA_SESSION_KEY);
        String sessionUsername = (String) session.getAttribute(GITHUB_USERNAME_SESSION_KEY);

        // 세션에 데이터가 없거나 다른 사용자의 데이터인 경우 데이터를 다시 로드
        if (events == null || !username.equals(sessionUsername)) {
            try {
                events = githubService.getUserCommits(username);
                // 다시 로드한 데이터 세션에 저장 (선택 사항이지만 일관성을 위해 유지)
                session.setAttribute(GITHUB_DATA_SESSION_KEY, events);
                session.setAttribute(GITHUB_USERNAME_SESSION_KEY, username);
            } catch (Exception e) {
                // 데이터 로드 실패 시 에러 페이지로 이동
                model.addAttribute("error", "Failed to fetch commit history: " + e.getMessage());
                model.addAttribute("exception", e.toString());
                return "error";
            }
        }

        // 데이터가 로드되었거나 세션에 있었으면 모델에 추가하고 메인 페이지 반환
        model.addAttribute("events", events);
        model.addAttribute("username", username);
        return "github/commits";
    }

    @GetMapping("/github/commits/data")
    @ResponseBody
    public Map<String, Object> getCommitData(@RequestParam(name = "username", defaultValue = "Red-Opera") String username,
                                           HttpSession session) 
    {
        try 
        {
            // 이 엔드포인트는 로딩 페이지에서 최초 데이터 로드를 위해 호출됨
            // 데이터를 가져와 세션에 저장하는 역할만 수행
            List<Map<String, Object>> events = githubService.getUserCommits(username);
            
            session.setAttribute(GITHUB_DATA_SESSION_KEY, events);
            session.setAttribute(GITHUB_USERNAME_SESSION_KEY, username);

            return Map.of(
                "success", true,
                "username", username
            );
        } 
        catch (Exception e) 
        {
            return Map.of(
                "success", false,
                "error", "Failed to fetch commit history: " + e.getMessage(),
                "exception", e.toString()
            );
        }
    }

    @GetMapping("/error")
    public String handleError(Model model) 
    {
        model.addAttribute("error", "An unexpected error occurred");
        return "error";
    }
} 