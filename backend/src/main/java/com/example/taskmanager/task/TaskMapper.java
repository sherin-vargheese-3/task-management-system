package com.example.taskmanager.task;

import com.example.taskmanager.task.dto.AssigneeResponse;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.user.User;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

  public TaskResponse toResponse(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getPriority(),
        task.getDueDate(),
        toAssignee(task.getAssignee()),
        task.getCompletedAt(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }

  private AssigneeResponse toAssignee(User user) {
    return user == null ? null : new AssigneeResponse(user.getId(), user.getName());
  }
}
