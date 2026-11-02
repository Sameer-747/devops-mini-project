package com.devops.taskmanager.model;

import java.time.Instant;

public class Task {

    private Long id;
    private String title;
    private Priority priority;
    private boolean completed;
    private Instant createdAt;

    public enum Priority { LOW, MEDIUM, HIGH }

    public Task() {
    }

    public Task(Long id, String title, Priority priority) {
        this.id = id;
        this.title = title;
        this.priority = priority;
        this.completed = false;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
