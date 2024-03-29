package com.sparta.topster.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sparta.topster.domain.user.dto.getUser.GetUserRes;
import com.sparta.topster.domain.user.dto.login.LoginReq;
import com.sparta.topster.domain.user.dto.signup.SignupReq;
import com.sparta.topster.domain.user.dto.signup.SignupRes;
import com.sparta.topster.domain.user.dto.update.UpdateReq;
import com.sparta.topster.domain.user.entity.User;
import com.sparta.topster.domain.user.entity.UserRoleEnum;
import com.sparta.topster.domain.user.repository.UserRepository;
import com.sparta.topster.domain.user.service.user.UserServiceImpl;
import com.sparta.topster.global.util.JwtUtil;
import com.sparta.topster.global.util.RedisUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceTest {

    @InjectMocks
    UserServiceImpl userService;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    RedisUtil redisUtil;

    @Mock
    JwtUtil jwtUtil;

    @Mock
    HttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("회원가입")
    @Order(1)
    @Test
    void signupTest() {
        //given
        String username = "username";
        String password = "password";
        String email = "username@naver.com";
        String nickname = "nickname";
        String intro = "intro";
        String certificationCode = "12345";

        SignupReq signupReq = SignupReq.builder()
            .username(username)
            .password(password)
            .email(email)
            .nickname(nickname)
            .intro(intro)
            .certification(certificationCode)
            .build();

        when(passwordEncoder.encode(password)).thenReturn(password);
        when(redisUtil.getData(email)).thenReturn(certificationCode);

        //when
        User user = User.builder().username(username).password(passwordEncoder.encode(password))
            .email(email)
            .nickname(nickname).intro(intro).build();

        when(userRepository.save(any(User.class))).thenReturn(user);

        SignupRes saveUser = userService.signUp(signupReq);
        //then
        assertEquals(saveUser.getUsername(), signupReq.getUsername());
        assertThatCode(() -> userService.signUp(signupReq)).doesNotThrowAnyException();
    }

    @DisplayName("로그인")
    @Order(2)
    @Test
    void LoginTest() {
        LoginReq loginReq = new LoginReq("username", "password");
        User mockUser = User.builder()
            .username(loginReq.getUsername())
            .password(loginReq.getPassword())
            .role(UserRoleEnum.USER)
            .build();

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.createToken(anyString(), any())).thenReturn("mockedToken");

        userService.loginUser(loginReq, response);

        verify(response, times(1)).setHeader(eq(JwtUtil.AUTHORIZATION_HEADER), eq("mockedToken"));
    }

    @DisplayName("수정")
    @Order(3)
    @Test
    void updateTest() {
        //given
        String username = "username";
        String password = "password";
        String newNickname = "newNickname";
        String newIntro = "newIntro";
        String encodePassword = passwordEncoder.encode(password);

        UpdateReq updateReq = new UpdateReq(newNickname, password, newIntro);

        User user = User.builder()
            .username(username)
            .password(encodePassword)
            .nickname("nickname")
            .intro("intro")
            .build();

        when(userRepository.findById(user.getId())).thenReturn(user);
        when(passwordEncoder.matches(eq(password), eq(encodePassword))).thenReturn(true);

        //when
        userService.updateUser(user, updateReq);

        //then
        assertEquals(newNickname, user.getNickname());
    }

    @Test
    @Order(4)
    @DisplayName("프로필 조회")
    void getUser() {
        //given
        User user = User.builder()
            .username("username")
            .password("password")
            .intro("Intro")
            .build();

        //when
        GetUserRes getUser = userService.getUser(user);

        //then
        assertThat(getUser.getNickname()).isEqualTo(user.getNickname());
    }

    @Test
    @Order(5)
    void refreshToken() {
        //given
        String refreshToken = "sampleRefreshToken";
        String username = "username";
        User user = User.builder()
            .username(username)
            .role(UserRoleEnum.USER)
            .build();

        //when
        when(redisUtil.getData(refreshToken)).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtUtil.createToken(username,user.getRole())).thenReturn("sampleToken");
        String token = jwtUtil.createToken(username, user.getRole());

        //then
        assertThat(token).isEqualTo("sampleToken");
        verify(jwtUtil, times(1)).createToken(username,user.getRole());
    }

    //    @Test
//    @DisplayName("회원탈퇴")
//    void deleteTest() {
//        //given
//        String username = "username";
//        String password = "password";
//        String encodePassword = passwordEncoder.encode(password);
//
//        DeleteReq deleteReq = new DeleteReq(password);
//
//        User user = User.builder()
//            .username(username)
//            .password(encodePassword)
//            .nickname("nickname")
//            .intro("intro")
//            .build();
//
//        when(userRepository.findById(user.getId())).thenReturn(user);
//        when(passwordEncoder.matches(eq(password), eq(encodePassword))).thenReturn(true);
//        //when
//        userService.deleteUser(user,deleteReq);
//
//        //then
//        assertThat(user).isNull();
//    }
}
