package com.springboot.model;

public class VisitorCount 
{
    private int todayCount;
    private int totalCount;

    public VisitorCount(int todayCount, int totalCount) 
    {
        this.todayCount = todayCount;
        this.totalCount = totalCount;
    }

    public int getTodayCount() 
    {
        return todayCount;
    }

    public void setTodayCount(int todayCount) 
    {
        this.todayCount = todayCount;
    }

    public int getTotalCount() 
    {
        return totalCount;
    }

    public void setTotalCount(int totalCount) 
    {
        this.totalCount = totalCount;
    }
} 