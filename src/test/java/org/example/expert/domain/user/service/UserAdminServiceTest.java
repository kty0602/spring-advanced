package org.example.expert.domain.user.service;

import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
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

import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserAdminServiceTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserAdminService userAdminService;

    @Test
    @DisplayName("권한 변경 성공")
    public void changeRole_success() {
        // given
        User user = new User("test@example.com","1234", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 3L);

        UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

        // when
        userAdminService.changeUserRole(user.getId(), request);

        // then
        assertEquals(UserRole.ADMIN, user.getUserRole());
    }

    @Test
    @DisplayName("권한 변경 시도하려는 사용자 정보를 찾을 수 없음")
    public void changeRole_notFoundUser() {
        // given
        User user = new User("test@example.com","1234", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 3L);
        UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");
        given(userRepository.findById(user.getId())).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
           userAdminService.changeUserRole(user.getId(), request);
        });

        // then
        assertEquals("User not found", exception.getMessage());
    }
}
