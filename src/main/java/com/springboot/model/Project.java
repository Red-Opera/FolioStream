package com.springboot.model;

public class Project 
{
    private String title;
    private String description;
    private String releaseStatus;

    public Project(String title, String description) 
    {
        this.title = title;
        this.description = description;
        this.releaseStatus = "released";
    }

    public Project(String title, String description, String releaseStatus) 
    {
        this.title = title;
        this.description = description;
        this.releaseStatus = releaseStatus;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getReleaseStatus() 
    {
        return releaseStatus;
    }

    public void setReleaseStatus(String releaseStatus) 
    {
        this.releaseStatus = releaseStatus;
    }
} 