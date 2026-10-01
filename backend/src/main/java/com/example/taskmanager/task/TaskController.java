package com.example.taskmanager.task;

import com.example.taskmanager.common.dto.PageResponse;
import com.example.taskmanager.task.dto.TaskAssignRequest;
import com.example.taskmanager.task.dto.TaskCreateRequest;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.task.dto.TaskSummaryResponse;
import com.example.taskmanager.task.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

  private final TaskService taskService;

  @GetMapping
  public PageResponse<TaskResponse> search(
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) TaskPriority priority,
      @RequestParam(required = false) @Positive Long assigneeId,
      @RequestParam(defaultValue = "false") boolean unassigned,
      @RequestParam(required = false) @Size(max = 100) String search,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    TaskFilter filter = new TaskFilter(status, priority, assigneeId, unassigned, search);
    return taskService.search(filter, pageable);
  }

  @GetMapping("/summary")
  public TaskSummaryResponse summary() {
    return taskService.summary();
  }

  @GetMapping("/{id}")
  public TaskResponse get(@PathVariable @Positive Long id) {
    return taskService.get(id);
  }

  @PostMapping
  public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request) {
    TaskResponse created = taskService.create(request);
    return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
  }

  @PutMapping("/{id}")
  public TaskResponse update(
      @PathVariable @Positive Long id, @Valid @RequestBody TaskUpdateRequest request) {
    return taskService.update(id, request);
  }

  @PatchMapping("/{id}/assignee")
  public TaskResponse assign(
      @PathVariable @Positive Long id, @Valid @RequestBody TaskAssignRequest request) {
    return taskService.assign(id, request.assigneeId());
  }

  @PatchMapping("/{id}/complete")
  public TaskResponse complete(@PathVariable @Positive Long id) {
    return taskService.complete(id);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
    taskService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
