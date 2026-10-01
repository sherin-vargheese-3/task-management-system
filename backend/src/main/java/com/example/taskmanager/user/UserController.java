package com.example.taskmanager.user;

import com.example.taskmanager.common.dto.PageResponse;
import com.example.taskmanager.user.dto.UserCreateRequest;
import com.example.taskmanager.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  public PageResponse<UserResponse> list(
      @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
    return userService.list(pageable);
  }

  @PostMapping
  public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
    UserResponse created = userService.create(request);
    return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
  }
}
