package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    public void signup_success() {
        // given
        SignupRequest signupRequest = new SignupRequest("test@example.com", "1234", "USER");
        String encoderPassword = "encodedPassword";
        String token = "testToken";
        UserRole userRole = UserRole.valueOf(signupRequest.getUserRole());

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(signupRequest.getPassword())).willReturn(encoderPassword);

        User saveUser = new User(signupRequest.getEmail(), encoderPassword, userRole);
        ReflectionTestUtils.setField(saveUser, "id", 3L);

        given(userRepository.save(any(User.class))).willReturn(saveUser);
        given(jwtUtil.createToken(3L, "test@example.com", userRole)).willReturn(token);

        // when
        SignupResponse response = authService.signup(signupRequest);

        // then
        assertNotNull(response);
        assertEquals(token, response.getBearerToken());
        verify(userRepository, times(1)).existsByEmail(signupRequest.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 시 이메일이 입력되지 않음")
    public void signup_notType_Email() {
        // given
        SignupRequest signupRequest = new SignupRequest("", "1234", "USER");

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                authService.signup(signupRequest);
        });

        // then
        assertEquals("이메일이 입력되지 않았습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("이메일이 이미 존재함")
    public void signup_alReadyExists_Email() {
        // given
        SignupRequest signupRequest = new SignupRequest("kty1467@naver.com", "1234", "USER");

        given(userRepository.existsByEmail(anyString())).willReturn(true);

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                authService.signup(signupRequest);
        });

        // then
        assertEquals("이미 존재하는 이메일입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("로그인 성공")
    public void signin_success() {
        // given
        SigninRequest signinRequest = new SigninRequest("kty1467@naver.com", "1234");
        String encoderPassword = "encodedPassword";
        String token = "testToken";
        User user = new User("kty1467@naver.com", encoderPassword, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 3L);

        given(userRepository.findByEmail(signinRequest.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(signinRequest.getPassword(), user.getPassword())).willReturn(true);
        given(jwtUtil.createToken(user.getId(), user.getEmail(), user.getUserRole())).willReturn(token);

        // when
        SigninResponse response = authService.signin(signinRequest);

        // then
        assertNotNull(response);
        assertEquals(token, response.getBearerToken());
        verify(userRepository, times(1)).findByEmail(signinRequest.getEmail());
        verify(passwordEncoder, times(1)).matches(signinRequest.getPassword(), encoderPassword);
        verify(jwtUtil, times(1)).createToken(user.getId(), user.getEmail(), user.getUserRole());
    }

    @Test
    @DisplayName("가입되지 않은 유저의 로그인 시도")
    public void signin_userNotFound() {
        // given
        SigninRequest signinRequest = new SigninRequest("notUser@example.com", "1234");

        given(userRepository.findByEmail(signinRequest.getEmail())).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            authService.signin(signinRequest);
        });

        // then
        assertEquals("가입되지 않은 유저입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 인한 로그인 시도")
    public void signin_incorrectPassword() {
        // given
        SigninRequest request = new SigninRequest("test@example.com", "12345");
        User user = new User("test@example.com", "1234", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 3L);

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPassword())).willReturn(false);

        // when
        AuthException exception = assertThrows(AuthException.class, () -> {
           authService.signin(request);
        });

        // then
        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
    }


}
