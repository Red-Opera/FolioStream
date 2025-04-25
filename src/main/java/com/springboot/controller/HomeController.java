package com.springboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class HomeController {

    private Map<String, List<String>> createPortfolioData() 
    {
        Map<String, List<String>> portfolios = new HashMap<>();
        portfolios.put("unity", Arrays.asList("Legacy of Auras", "K Project", "셔틀버스 디펜스 게임"));
        portfolios.put("unreal", Arrays.asList("Era of Dreams : 1950s Simulation", "Unreal Project 2", "Unreal Project 3"));
        portfolios.put("graphic", Arrays.asList("DirectX GameEngine", "D2DGame", "Sokoban"));
        
        return portfolios;
    }

    @GetMapping("/")
    public String home(Model model) 
    {
        model.addAttribute("message", "포트폴리오 갤러리");
        model.addAttribute("portfolios", createPortfolioData());
        model.addAttribute("selectedCategory", null);
        
        return "home";
    }

    @GetMapping("/category/{category}")
    public String category(@PathVariable("category") String category, Model model) 
    {
        model.addAttribute("message", "포트폴리오 갤러리");
        model.addAttribute("portfolios", createPortfolioData());
        model.addAttribute("selectedCategory", category);
        
        return "home";
    }
}