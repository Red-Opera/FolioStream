package com.springboot.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class UniqueVisitor 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private String visitorIdentifier; // IP + user agent 해시

    public UniqueVisitor() {}

    public UniqueVisitor(LocalDate date, String visitorIdentifier) 
    {
        this.date = date;
        this.visitorIdentifier = visitorIdentifier;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getVisitorIdentifier() { return visitorIdentifier; }
    public void setVisitorIdentifier(String visitorIdentifier) { this.visitorIdentifier = visitorIdentifier; }
}
