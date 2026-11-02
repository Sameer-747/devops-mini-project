package com.devops.taskmanager.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for creating or updating a task. */
public record TaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,
        Task.Priority priority,
        Boolean completed) {
}
