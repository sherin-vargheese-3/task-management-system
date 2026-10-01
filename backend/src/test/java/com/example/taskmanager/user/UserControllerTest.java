package com.example.taskmanager.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taskmanager.common.dto.PageResponse;
import com.example.taskmanager.common.exception.ConflictException;
import com.example.taskmanager.user.dto.UserCreateRequest;
import com.example.taskmanager.user.dto.UserResponse;
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

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  @Test
  void listReturnsPagedUsersSortedByNameByDefault() throws Exception {
    when(userService.list(any(Pageable.class)))
        .thenReturn(
            new PageResponse<>(List.of(new UserResponse(1L, "Alice", "alice@example.com")), 0, 50, 1, 1, true));
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("Alice"))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(userService).list(pageable.capture());
    assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "name"));
  }

  @Test
  void createReturns201WithLocation() throws Exception {
    when(userService.create(any(UserCreateRequest.class)))
        .thenReturn(new UserResponse(4L, "Dan", "dan@example.com"));

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dan\",\"email\":\"dan@example.com\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/users/4"))
        .andExpect(jsonPath("$.id").value(4));
  }

  @Test
  void createWithInvalidEmailReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dan\",\"email\":\"not-an-email\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("email"));

    verifyNoInteractions(userService);
  }

  @Test
  void createWithDuplicateEmailReturns409() throws Exception {
    when(userService.create(any(UserCreateRequest.class)))
        .thenThrow(new ConflictException("A user with this email already exists"));

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("A user with this email already exists"));
  }
}
