package com.springboot.model;

public class Feature {
    private String id;
    private String icon;
    private String title;
    private String description;
    private String[] techTags;

    public Feature() {}

    public Feature(String id, String icon, String title, String description, String[] techTags) {
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.techTags = techTags;
    }

    public String getId() { return id; }
    public String getIcon() { return icon; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String[] getTechTags() { return techTags; }
}
