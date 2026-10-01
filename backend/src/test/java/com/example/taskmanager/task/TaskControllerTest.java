package com.example.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taskmanager.common.dto.PageResponse;
import com.example.taskmanager.common.exception.ConflictException;
import com.example.taskmanager.common.exception.ResourceNotFoundException;
import com.example.taskmanager.task.dto.AssigneeResponse;
import com.example.taskmanager.task.dto.TaskCreateRequest;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.task.dto.TaskSummaryResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

  private static final Instant CREATED = Instant.parse("2026-10-01T10:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TaskService taskService;

  @Test
  void createReturns201WithLocationAndBody() throws Exception {
    when(taskService.create(any(TaskCreateRequest.class))).thenReturn(response(15L, TaskStatus.TODO));

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Write docs\",\"priority\":\"HIGH\",\"assigneeId\":1}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/tasks/15"))
        .andExpect(jsonPath("$.id").value(15))
        .andExpect(jsonPath("$.status").value("TODO"))
        .andExpect(jsonPath("$.assignee.name").value("Alice"));
  }

  @Test
  void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"  \",\"assigneeId\":-1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(
            jsonPath("$.errors[*].field").value(containsInAnyOrder("title", "priority", "assigneeId")));

    verifyNoInteractions(taskService);
  }

  @Test
  void createWithUnknownEnumValueReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"priority\":\"URGENT\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(taskService);
  }

  @Test
  void createWithTooLongTitleReturns400() throws Exception {
    String title = "a".repeat(151);

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"priority\":\"LOW\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("title"));
  }

  @Test
  void getMissingTaskReturns404ProblemDetail() throws Exception {
    when(taskService.get(99L)).thenThrow(new ResourceNotFoundException("Task", 99L));

    mockMvc
        .perform(get("/api/tasks/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Task with id 99 not found"));
  }

  @Test
  void getWithNonPositiveIdReturns400() throws Exception {
    mockMvc
        .perform(get("/api/tasks/0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("id"));

    verifyNoInteractions(taskService);
  }

  @Test
  void searchBindsFiltersAndPageable() throws Exception {
    when(taskService.search(any(TaskFilter.class), any(Pageable.class)))
        .thenReturn(new PageResponse<>(List.of(response(1L, TaskStatus.IN_PROGRESS)), 2, 5, 11, 3, true));
    ArgumentCaptor<TaskFilter> filter = ArgumentCaptor.forClass(TaskFilter.class);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

    mockMvc
        .perform(
            get("/api/tasks")
                .param("status", "IN_PROGRESS")
                .param("priority", "HIGH")
                .param("assigneeId", "3")
                .param("search", "docs")
                .param("page", "2")
                .param("size", "5")
                .param("sort", "dueDate,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.totalElements").value(11))
        .andExpect(jsonPath("$.page").value(2));

    verify(taskService).search(filter.capture(), pageable.capture());
    assertThat(filter.getValue())
        .isEqualTo(new TaskFilter(TaskStatus.IN_PROGRESS, TaskPriority.HIGH, 3L, false, "docs"));
    assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
    assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    assertThat(pageable.getValue().getSort())
        .isEqualTo(Sort.by(Sort.Direction.ASC, "dueDate"));
  }

  @Test
  void searchWithInvalidStatusReturns400() throws Exception {
    mockMvc
        .perform(get("/api/tasks").param("status", "NOPE"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Invalid value 'NOPE' for parameter 'status'"));
  }

  @Test
  void updateWithoutStatusReturns400() throws Exception {
    mockMvc
        .perform(
            put("/api/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"priority\":\"LOW\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("status"));
  }

  @Test
  void assignPassesAssigneeToService() throws Exception {
    when(taskService.assign(eq(1L), eq(2L))).thenReturn(response(1L, TaskStatus.TODO));

    mockMvc
        .perform(
            patch("/api/tasks/1/assignee")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\":2}"))
        .andExpect(status().isOk());

    verify(taskService).assign(1L, 2L);
  }

  @Test
  void completeAlreadyCompletedReturns409() throws Exception {
    when(taskService.complete(1L)).thenThrow(new ConflictException("Task 1 is already completed"));

    mockMvc
        .perform(patch("/api/tasks/1/complete"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Task 1 is already completed"));
  }

  @Test
  void deleteReturns204() throws Exception {
    mockMvc.perform(delete("/api/tasks/1")).andExpect(status().isNoContent());

    verify(taskService).delete(1L);
  }

  @Test
  void summaryReturnsCounts() throws Exception {
    when(taskService.summary()).thenReturn(new TaskSummaryResponse(6, 3, 2, 1));

    mockMvc
        .perform(get("/api/tasks/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(6))
        .andExpect(jsonPath("$.inProgress").value(2));
  }

  @Test
  void unexpectedErrorReturnsGeneric500WithoutInternals() throws Exception {
    when(taskService.get(1L)).thenThrow(new IllegalStateException("db password=secret"));

    mockMvc
        .perform(get("/api/tasks/1"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
  }

  private static TaskResponse response(Long id, TaskStatus status) {
    return new TaskResponse(
        id,
        "Write docs",
        null,
        status,
        TaskPriority.HIGH,
        null,
        new AssigneeResponse(1L, "Alice"),
        null,
        CREATED,
        CREATED);
  }
}
