package com.springboot.model;

import java.util.List;

public class CodeFile {
    private String icon;
    private String name;
    private String path;
    private String status;
    private String description;
    private List<String> features;

    public CodeFile() {}

    public CodeFile(String icon, String name, String path, String status, String description, List<String> features) 
    {
        this.icon = icon;
        this.name = name;
        this.path = path;
        this.status = status;
        this.description = description;
        this.features = features;
    }

    public void setName(String name) { this.name = name; }
    public void setPath(String path) { this.path = path; }

    public String getIcon() { return icon; }
    public String getName() { return name; }
    public String getPath() { return path; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public List<String> getFeatures() { return features; }
}
