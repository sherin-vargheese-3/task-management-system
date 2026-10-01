package com.example.taskmanager.task.dto;

public record TaskSummaryResponse(long total, long todo, long inProgress, long done) {}
