package com.example.taskmanager.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskmanager.common.dto.PageResponse;
import com.example.taskmanager.common.exception.ConflictException;
import com.example.taskmanager.common.exception.ResourceNotFoundException;
import com.example.taskmanager.user.dto.UserCreateRequest;
import com.example.taskmanager.user.dto.UserResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserService userService;

  @Test
  void listMapsRepositoryPageToResponse() {
    PageRequest pageable = PageRequest.of(0, 2);
    User alice = new User("Alice", "alice@example.com");
    ReflectionTestUtils.setField(alice, "id", 1L);
    when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(alice), pageable, 3));

    PageResponse<UserResponse> page = userService.list(pageable);

    assertThat(page.content()).containsExactly(new UserResponse(1L, "Alice", "alice@example.com"));
    assertThat(page.totalElements()).isEqualTo(3);
    assertThat(page.totalPages()).isEqualTo(2);
    assertThat(page.last()).isFalse();
  }

  @Test
  void createNormalisesEmailAndSaves() {
    when(userRepository.existsByEmailIgnoreCase("dan@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", 4L);
              return saved;
            });

    UserResponse response = userService.create(new UserCreateRequest(" Dan ", " Dan@Example.com "));

    assertThat(response).isEqualTo(new UserResponse(4L, "Dan", "dan@example.com"));
  }

  @Test
  void createWithDuplicateEmailThrowsConflict() {
    when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

    assertThrows(
        ConflictException.class,
        () -> userService.create(new UserCreateRequest("Alice", "alice@example.com")));

    verify(userRepository, never()).save(any());
  }

  @Test
  void getEntityMissingThrowsNotFound() {
    when(userRepository.findById(42L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> userService.getEntity(42L));
  }
}
