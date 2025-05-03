package com.springboot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "visitor_stats")
public class VisitorStats 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDate date;
    private int dailyVisitors;
    private int totalVisitors;

    public VisitorStats() 
    {
    	
    }

    public VisitorStats(LocalDate date, int dailyVisitors, int totalVisitors) 
    {
        this.date = date;
        this.dailyVisitors = dailyVisitors;
        this.totalVisitors = totalVisitors;
    }

    // Getters and Setters
    public Long getId() 
    {
        return id;
    }

    public void setId(Long id) 
    {
        this.id = id;
    }

    public LocalDate getDate() 
    {
        return date;
    }

    public void setDate(LocalDate date) 
    {
        this.date = date;
    }

    public int getDailyVisitors() 
    {
        return dailyVisitors;
    }

    public void setDailyVisitors(int dailyVisitors) 
    {
        this.dailyVisitors = dailyVisitors;
    }

    public int getTotalVisitors() 
    {
        return totalVisitors;
    }

    public void setTotalVisitors(int totalVisitors) 
    {
        this.totalVisitors = totalVisitors;
    }
} 