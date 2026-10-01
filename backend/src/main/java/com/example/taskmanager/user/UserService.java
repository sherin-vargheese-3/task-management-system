package com.example.taskmanager.user;

import com.example.taskmanager.common.dto.PageResponse;
import com.example.taskmanager.common.exception.ConflictException;
import com.example.taskmanager.common.exception.ResourceNotFoundException;
import com.example.taskmanager.user.dto.UserCreateRequest;
import com.example.taskmanager.user.dto.UserResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public PageResponse<UserResponse> list(Pageable pageable) {
    return PageResponse.from(userRepository.findAll(pageable), UserResponse::from);
  }

  @Transactional
  public UserResponse create(UserCreateRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new ConflictException("A user with this email already exists");
    }
    User saved = userRepository.save(new User(request.name().trim(), email));
    log.info("Created user id={}", saved.getId());
    return UserResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public User getEntity(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
  }
}
