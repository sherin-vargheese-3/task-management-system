package com.example.taskmanager.task;

import com.example.taskmanager.common.dto.PageResponse;
import com.example.taskmanager.common.exception.ConflictException;
import com.example.taskmanager.common.exception.ResourceNotFoundException;
import com.example.taskmanager.task.dto.TaskCreateRequest;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.task.dto.TaskSummaryResponse;
import com.example.taskmanager.task.dto.TaskUpdateRequest;
import com.example.taskmanager.user.User;
import com.example.taskmanager.user.UserService;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

  private final TaskRepository taskRepository;
  private final UserService userService;
  private final TaskMapper taskMapper;
  private final Clock clock;

  @Transactional(readOnly = true)
  public PageResponse<TaskResponse> search(TaskFilter filter, Pageable pageable) {
    return PageResponse.from(
        taskRepository.findAll(TaskSpecifications.matching(filter), pageable),
        taskMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public TaskResponse get(Long id) {
    return taskMapper.toResponse(findTask(id));
  }

  @Transactional(readOnly = true)
  public TaskSummaryResponse summary() {
    Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
    taskRepository.countByStatus().forEach(row -> counts.put(row.getStatus(), row.getCount()));
    long todo = counts.getOrDefault(TaskStatus.TODO, 0L);
    long inProgress = counts.getOrDefault(TaskStatus.IN_PROGRESS, 0L);
    long done = counts.getOrDefault(TaskStatus.DONE, 0L);
    return new TaskSummaryResponse(todo + inProgress + done, todo, inProgress, done);
  }

  @Transactional
  public TaskResponse create(TaskCreateRequest request) {
    Task task = new Task();
    task.setTitle(request.title().trim());
    task.setDescription(request.description());
    task.setPriority(request.priority());
    task.setDueDate(request.dueDate());
    task.setAssignee(resolveAssignee(request.assigneeId()));
    task.changeStatus(request.status() == null ? TaskStatus.TODO : request.status(), clock.instant());
    Task saved = taskRepository.save(task);
    log.info("Created task id={}", saved.getId());
    return taskMapper.toResponse(saved);
  }

  @Transactional
  public TaskResponse update(Long id, TaskUpdateRequest request) {
    Task task = findTask(id);
    task.setTitle(request.title().trim());
    task.setDescription(request.description());
    task.setPriority(request.priority());
    task.setDueDate(request.dueDate());
    task.setAssignee(resolveAssignee(request.assigneeId()));
    task.changeStatus(request.status(), clock.instant());
    log.info("Updated task id={}", id);
    return taskMapper.toResponse(taskRepository.saveAndFlush(task));
  }

  @Transactional
  public TaskResponse assign(Long id, Long assigneeId) {
    Task task = findTask(id);
    task.setAssignee(resolveAssignee(assigneeId));
    log.info("Assigned task id={} to userId={}", id, assigneeId);
    return taskMapper.toResponse(taskRepository.saveAndFlush(task));
  }

  @Transactional
  public TaskResponse complete(Long id) {
    Task task = findTask(id);
    if (task.isCompleted()) {
      throw new ConflictException("Task %d is already completed".formatted(id));
    }
    task.changeStatus(TaskStatus.DONE, clock.instant());
    log.info("Completed task id={}", id);
    return taskMapper.toResponse(taskRepository.saveAndFlush(task));
  }

  @Transactional
  public void delete(Long id) {
    Task task = findTask(id);
    taskRepository.delete(task);
    log.info("Deleted task id={}", id);
  }

  private Task findTask(Long id) {
    return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
  }

  private User resolveAssignee(Long assigneeId) {
    return assigneeId == null ? null : userService.getEntity(assigneeId);
  }
}
