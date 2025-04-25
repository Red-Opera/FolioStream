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

    private Map<String, List<String>> createPortfolioData() {
        Map<String, List<String>> portfolios = new HashMap<>();
        portfolios.put("unity", Arrays.asList("Unity Game 1", "Unity Game 2", "Unity Game 3"));
        portfolios.put("unreal", Arrays.asList("Unreal Project 1", "Unreal Project 2", "Unreal Project 3"));
        portfolios.put("graphic", Arrays.asList("Graphic Design 1", "Graphic Design 2", "Graphic Design 3"));
        return portfolios;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "포트폴리오 갤러리");
        model.addAttribute("portfolios", createPortfolioData());
        model.addAttribute("selectedCategory", null);
        return "home";
    }

    @GetMapping("/category/{category}")
    public String category(@PathVariable("category") String category, Model model) {
        model.addAttribute("message", "포트폴리오 갤러리");
        model.addAttribute("portfolios", createPortfolioData());
        model.addAttribute("selectedCategory", category);
        return "home";
    }
}