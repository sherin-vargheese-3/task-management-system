package com.example.taskmanager.task.dto;

import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskCreateRequest(
    @NotBlank @Size(max = 150) String title,
    @Size(max = 2000) String description,
    TaskStatus status,
    @NotNull TaskPriority priority,
    LocalDate dueDate,
    @Positive Long assigneeId) {}
