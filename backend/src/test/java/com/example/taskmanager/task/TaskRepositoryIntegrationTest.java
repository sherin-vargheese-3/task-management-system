package com.example.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.example.taskmanager.user.User;
import com.example.taskmanager.user.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired private TaskRepository taskRepository;
  @Autowired private UserRepository userRepository;

  private User alice;

  @BeforeEach
  void setUp() {
    taskRepository.deleteAll();
    alice = userRepository.save(new User("Alice Test", "alice.test@example.com"));
    User bob = userRepository.save(new User("Bob Test", "bob.test@example.com"));
    save("Write API docs", TaskStatus.TODO, TaskPriority.HIGH, alice);
    save("Fix 100% CPU bug", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, bob);
    save("Plan sprint", TaskStatus.DONE, TaskPriority.LOW, alice);
    save("Unowned chore", TaskStatus.TODO, TaskPriority.MEDIUM, null);
  }

  @Test
  void filtersByStatusPriorityAndAssignee() {
    TaskFilter filter = new TaskFilter(TaskStatus.TODO, TaskPriority.HIGH, alice.getId(), false, null);

    Page<Task> page = taskRepository.findAll(TaskSpecifications.matching(filter), PageRequest.of(0, 10));

    assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("Write API docs");
  }

  @Test
  void filtersUnassignedTasks() {
    TaskFilter filter = new TaskFilter(null, null, null, true, null);

    Page<Task> page = taskRepository.findAll(TaskSpecifications.matching(filter), PageRequest.of(0, 10));

    assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("Unowned chore");
  }

  @Test
  void searchIsCaseInsensitiveAndTreatsWildcardsLiterally() {
    TaskFilter percent = new TaskFilter(null, null, null, false, "100%");
    TaskFilter mixedCase = new TaskFilter(null, null, null, false, "pLAN");

    Page<Task> percentMatches =
        taskRepository.findAll(TaskSpecifications.matching(percent), PageRequest.of(0, 10));
    Page<Task> caseMatches =
        taskRepository.findAll(TaskSpecifications.matching(mixedCase), PageRequest.of(0, 10));

    assertThat(percentMatches.getContent()).extracting(Task::getTitle).containsExactly("Fix 100% CPU bug");
    assertThat(caseMatches.getContent()).extracting(Task::getTitle).containsExactly("Plan sprint");
  }

  @Test
  void paginatesAndSortsWithAssigneeLoaded() {
    TaskFilter noFilter = new TaskFilter(null, null, null, false, null);

    Page<Task> page =
        taskRepository.findAll(
            TaskSpecifications.matching(noFilter), PageRequest.of(0, 3, Sort.by("title")));

    assertThat(page.getTotalElements()).isEqualTo(4);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(page.getContent())
        .extracting(Task::getTitle)
        .containsExactly("Fix 100% CPU bug", "Plan sprint", "Unowned chore");
    assertThat(page.getContent().getFirst().getAssignee().getName()).isEqualTo("Bob Test");
  }

  @Test
  void countsTasksByStatus() {
    var counts = taskRepository.countByStatus();

    assertThat(counts)
        .extracting(TaskRepository.StatusCount::getStatus, TaskRepository.StatusCount::getCount)
        .containsExactlyInAnyOrder(
            tuple(TaskStatus.TODO, 2L),
            tuple(TaskStatus.IN_PROGRESS, 1L),
            tuple(TaskStatus.DONE, 1L));
  }

  private void save(String title, TaskStatus status, TaskPriority priority, User assignee) {
    Task task = new Task();
    task.setTitle(title);
    task.setPriority(priority);
    task.setAssignee(assignee);
    task.changeStatus(status, Instant.parse("2026-10-01T10:00:00Z"));
    taskRepository.save(task);
  }
}
