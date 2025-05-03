package com.springboot.controller;

import com.springboot.data.VisitorService;
import com.springboot.model.VisitorStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visitors")
public class VisitorController 
{

    private final VisitorService visitorService;

    @Autowired
    public VisitorController(VisitorService visitorService) 
    {
        this.visitorService = visitorService;
    }

    @GetMapping("/stats")
    public VisitorStats getVisitorStats() 
    {
        return visitorService.getVisitorStats();
    }

    @GetMapping("/increment")
    public void incrementVisitor() 
    {
        visitorService.incrementDailyVisitors();
    }
} 