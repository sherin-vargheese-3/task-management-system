package com.example.taskmanager.task.dto;

import jakarta.validation.constraints.Positive;

public record TaskAssignRequest(@Positive Long assigneeId) {}
