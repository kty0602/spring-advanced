package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("유저 정보 성공적으로 조회")
    public void getUser_success() {
        // given
        long userId = 1L;
        AuthUser authUser = new AuthUser(1L, "test1@example.com", UserRole.USER);
        User user = User.fromAuthUser(authUser);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.getUser(userId);

        // then
        assertNotNull(response);
        assertEquals(user.getId(), response.getId());
        assertEquals(user.getEmail(), response.getEmail());
    }

    @Test
    @DisplayName("유저 정보 조회 실패")
    public void getUser_fail() {
        // given
        long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            userService.getUser(userId);
        });

        // then
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("비밀번호 성공적으로 변경")
    public void changePassword_success() {
        // given
        String oldPassword = "oldPassword1";
        String newPassword = "newPassword1";
        String encoderPassword = passwordEncoder.encode(newPassword);

        User user = new User("test@example.com", passwordEncoder.encode(oldPassword), UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 3L);
        UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, newPassword);

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(oldPassword, user.getPassword())).willReturn(true);
        given(passwordEncoder.encode(newPassword)).willReturn(encoderPassword);
        given(passwordEncoder.matches(newPassword, user.getPassword())).willReturn(false);

        // when
        userService.changePassword(3L, request);

        // then
        assertEquals(encoderPassword, user.getPassword());
    }

    @Test
    @DisplayName("새 비밀번호와 기존 비밀번호 동일")
    public void changePassword_fail_samePassword() {
        // given
        String oldPassword = "oldPassword1";
        String newPassword = oldPassword;
        User user = new User("test@example.com", oldPassword, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 3L);
        UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, newPassword);

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(oldPassword, user.getPassword())).willReturn(true);

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            userService.changePassword(3L, request);
        });

        // then
        assertEquals("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("기존 비밀번호를 잘못 입력")
    public void changePassword_fail_wrongPassword() {
        // given
        String oldPassword = "oldPassword1";
        String newPassword = "newPassword1";
        User user = new User("test@example.com", oldPassword, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 3L);
        UserChangePasswordRequest request = new UserChangePasswordRequest("wrongPassword1", newPassword);

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword1", user.getPassword())).willReturn(false);
        // 실제 호출 관련 문제 발생 -> 이유 모름
        given(passwordEncoder.matches(newPassword, user.getPassword())).willReturn(false);

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            userService.changePassword(3L, request);
        });

        // then
        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("유효성 검증 실패")
    public void changePassword_failValid() {
        // given
        String oldPassword = "oldPassword";
        String newPassword = "11223344";
        User user = new User("test@example.com", oldPassword, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 3L);
        UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, newPassword);

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            userService.changePassword(3L, request);
        });

        // then
        assertEquals("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("변경 요청하는 유저 정보가 없음")
    public void changePassword_notFoundUser() {
        // given
        String oldPassword = "oldPassword1";
        String newPassword = "newPassword1";
        User user = new User("test@example.com", oldPassword, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 3L);
        UserChangePasswordRequest request = new UserChangePasswordRequest(oldPassword, newPassword);

        given(userRepository.findById(user.getId())).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
           userService.changePassword(user.getId(), request);
        });

        // then
        assertEquals("User not found", exception.getMessage());
    }
}
