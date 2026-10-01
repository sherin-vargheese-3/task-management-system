package com.example.taskmanager.task;

public record TaskFilter(
    TaskStatus status, TaskPriority priority, Long assigneeId, boolean unassigned, String search) {}
