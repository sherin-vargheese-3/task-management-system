package com.example.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskmanager.common.exception.ConflictException;
import com.example.taskmanager.common.exception.ResourceNotFoundException;
import com.example.taskmanager.task.dto.TaskCreateRequest;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.task.dto.TaskSummaryResponse;
import com.example.taskmanager.task.dto.TaskUpdateRequest;
import com.example.taskmanager.user.User;
import com.example.taskmanager.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

  @Mock private TaskRepository taskRepository;
  @Mock private UserService userService;

  private TaskService taskService;

  @BeforeEach
  void setUp() {
    taskService =
        new TaskService(
            taskRepository, userService, new TaskMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createDefaultsStatusToTodoAndAssignsUser() {
    User alice = user(1L, "Alice");
    when(userService.getEntity(1L)).thenReturn(alice);
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));
    TaskCreateRequest request =
        new TaskCreateRequest("  Write docs ", "details", null, TaskPriority.HIGH, LocalDate.of(2026, 10, 9), 1L);

    TaskResponse response = taskService.create(request);

    assertThat(response.id()).isEqualTo(10L);
    assertThat(response.title()).isEqualTo("Write docs");
    assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    assertThat(response.assignee().name()).isEqualTo("Alice");
    assertThat(response.completedAt()).isNull();
  }

  @Test
  void createAsDoneRecordsCompletionTime() {
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 11L));
    TaskCreateRequest request =
        new TaskCreateRequest("Done already", null, TaskStatus.DONE, TaskPriority.LOW, null, null);

    TaskResponse response = taskService.create(request);

    assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    assertThat(response.completedAt()).isEqualTo(NOW);
    assertThat(response.assignee()).isNull();
  }

  @Test
  void createWithUnknownAssigneeFailsWithoutSaving() {
    when(userService.getEntity(99L)).thenThrow(new ResourceNotFoundException("User", 99L));
    TaskCreateRequest request =
        new TaskCreateRequest("Task", null, null, TaskPriority.LOW, null, 99L);

    assertThrows(ResourceNotFoundException.class, () -> taskService.create(request));

    verify(taskRepository, never()).save(any());
  }

  @Test
  void getMissingTaskThrowsNotFound() {
    when(taskRepository.findById(5L)).thenReturn(Optional.empty());

    ResourceNotFoundException thrown =
        assertThrows(ResourceNotFoundException.class, () -> taskService.get(5L));

    assertThat(thrown.getMessage()).isEqualTo("Task with id 5 not found");
  }

  @Test
  void updateReplacesFieldsAndClearsCompletionWhenReopened() {
    Task task = existingTask(3L, TaskStatus.DONE);
    User bob = user(2L, "Bob");
    when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
    when(userService.getEntity(2L)).thenReturn(bob);
    when(taskRepository.saveAndFlush(task)).thenReturn(task);
    TaskUpdateRequest request =
        new TaskUpdateRequest("Renamed", "new", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null, 2L);

    TaskResponse response = taskService.update(3L, request);

    assertThat(response.title()).isEqualTo("Renamed");
    assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    assertThat(response.completedAt()).isNull();
    assertThat(response.assignee().id()).isEqualTo(2L);
  }

  @Test
  void assignWithNullAssigneeUnassignsTask() {
    Task task = existingTask(4L, TaskStatus.TODO);
    task.setAssignee(user(1L, "Alice"));
    when(taskRepository.findById(4L)).thenReturn(Optional.of(task));
    when(taskRepository.saveAndFlush(task)).thenReturn(task);

    TaskResponse response = taskService.assign(4L, null);

    assertThat(response.assignee()).isNull();
  }

  @Test
  void assignSetsNewAssignee() {
    Task task = existingTask(4L, TaskStatus.TODO);
    when(taskRepository.findById(4L)).thenReturn(Optional.of(task));
    when(userService.getEntity(3L)).thenReturn(user(3L, "Carol"));
    when(taskRepository.saveAndFlush(task)).thenReturn(task);

    TaskResponse response = taskService.assign(4L, 3L);

    assertThat(response.assignee().name()).isEqualTo("Carol");
  }

  @Test
  void completeMarksTaskDoneWithTimestamp() {
    Task task = existingTask(6L, TaskStatus.IN_PROGRESS);
    when(taskRepository.findById(6L)).thenReturn(Optional.of(task));
    when(taskRepository.saveAndFlush(task)).thenReturn(task);

    TaskResponse response = taskService.complete(6L);

    assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    assertThat(response.completedAt()).isEqualTo(NOW);
  }

  @Test
  void completeAlreadyCompletedTaskThrowsConflict() {
    when(taskRepository.findById(7L)).thenReturn(Optional.of(existingTask(7L, TaskStatus.DONE)));

    assertThrows(ConflictException.class, () -> taskService.complete(7L));

    verify(taskRepository, never()).saveAndFlush(any());
  }

  @Test
  void deleteRemovesExistingTask() {
    Task task = existingTask(8L, TaskStatus.TODO);
    when(taskRepository.findById(8L)).thenReturn(Optional.of(task));

    taskService.delete(8L);

    verify(taskRepository).delete(task);
  }

  @Test
  void deleteMissingTaskThrowsNotFound() {
    when(taskRepository.findById(9L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> taskService.delete(9L));

    verify(taskRepository, never()).delete(any(Task.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void searchMapsRepositoryPageToResponse() {
    PageRequest pageable = PageRequest.of(1, 2);
    List<Task> tasks = List.of(existingTask(1L, TaskStatus.TODO), existingTask(2L, TaskStatus.TODO));
    when(taskRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(tasks, pageable, 5));
    TaskFilter filter = new TaskFilter(TaskStatus.TODO, null, null, false, null);

    var page = taskService.search(filter, pageable);

    assertThat(page.content()).extracting(TaskResponse::id).containsExactly(1L, 2L);
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(2);
    assertThat(page.totalElements()).isEqualTo(5);
    assertThat(page.totalPages()).isEqualTo(3);
    assertThat(page.last()).isFalse();
  }

  @Test
  void summaryFillsMissingStatusesWithZero() {
    when(taskRepository.countByStatus())
        .thenReturn(List.of(statusCount(TaskStatus.TODO, 3), statusCount(TaskStatus.DONE, 2)));

    TaskSummaryResponse summary = taskService.summary();

    assertThat(summary).isEqualTo(new TaskSummaryResponse(5, 3, 0, 2));
  }

  private static Task existingTask(Long id, TaskStatus status) {
    Task task = new Task();
    task.setTitle("Task " + id);
    task.setPriority(TaskPriority.MEDIUM);
    task.changeStatus(status, NOW);
    return withId(task, id);
  }

  private static Task withId(Task task, Long id) {
    ReflectionTestUtils.setField(task, "id", id);
    return task;
  }

  private static User user(Long id, String name) {
    User user = new User(name, name.toLowerCase() + "@example.com");
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private static TaskRepository.StatusCount statusCount(TaskStatus status, long count) {
    return new TaskRepository.StatusCount() {
      @Override
      public TaskStatus getStatus() {
        return status;
      }

      @Override
      public long getCount() {
        return count;
      }
    };
  }
}
