package com.springboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.springboot.service.GitHubService;

import java.util.List;
import java.util.Map;

@Controller
public class GitHubController 
{
    @Autowired
    private GitHubService githubService;

    @GetMapping("/github/commits")
    public String getCommitHistory(@RequestParam(name = "username", defaultValue = "Red-Opera") String username, Model model) 
    {
    	System.out.println("GitHubController.getCommitHistory() called with username: " + username);
    	
        try 
        {
            List<Map<String, Object>> events = githubService.getUserCommits(username);
            model.addAttribute("events", events);
            model.addAttribute("username", username);
            return "github/commits";
        } 
        
        catch (Exception e) 
        {
            model.addAttribute("error", "Failed to fetch commit history: " + e.getMessage());
            model.addAttribute("exception", e.toString());

            // 스택트레이스 문자열로 변환
            StringBuilder sb = new StringBuilder();
            
            for (StackTraceElement ste : e.getStackTrace())
                sb.append(ste.toString()).append("\\n");

            model.addAttribute("stackTrace", sb.toString());
            
            return "error";
        }
    }

    @GetMapping("/error")
    public String handleError(Model model) 
    {
        model.addAttribute("error", "An unexpected error occurred");
        
        return "error";
    }
} 