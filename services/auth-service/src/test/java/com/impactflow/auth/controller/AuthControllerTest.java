package com.impactflow.auth.controller;

import com.impactflow.auth.domain.User;
import com.impactflow.auth.dto.AuthResponse;
import com.impactflow.auth.dto.LoginRequest;
import com.impactflow.auth.dto.RegisterRequest;
import com.impactflow.auth.repository.UserRepository;
import com.impactflow.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRegisterUserSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("password")
                .roles(Collections.singletonList("ROLE_DEVELOPER"))
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashedPassword");

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("User registered successfully.", response.getBody());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testRegisterUserUsernameTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .password("password")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Username is already taken.", response.getBody());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testAuthenticateUserSuccess() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password")
                .build();

        User user = User.builder()
                .username("testuser")
                .password("hashedPassword")
                .roles(Collections.singletonList("ROLE_DEVELOPER"))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(tokenProvider.generateToken("testuser", user.getRoles())).thenReturn("mockJwtToken");

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthResponse authResponse = (AuthResponse) response.getBody();
        assertEquals("mockJwtToken", authResponse.getToken());
        assertEquals("testuser", authResponse.getUsername());
    }

    @Test
    public void testAuthenticateUserFailure() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
