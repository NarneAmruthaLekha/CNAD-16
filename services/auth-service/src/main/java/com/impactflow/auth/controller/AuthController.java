package com.impactflow.auth.controller;

import com.impactflow.auth.domain.User;
import com.impactflow.auth.dto.AuthResponse;
import com.impactflow.auth.dto.LoginRequest;
import com.impactflow.auth.dto.RegisterRequest;
import com.impactflow.auth.repository.UserRepository;
import com.impactflow.auth.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username is already taken.");
        }

        List<String> roles = request.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = new ArrayList<>();
            roles.add("ROLE_DEVELOPER");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .build();

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.");
        }

        User user = userOpt.get();
        String jwt = tokenProvider.generateToken(user.getUsername(), user.getRoles());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwt)
                .username(user.getUsername())
                .roles(user.getRoles())
                .build());
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam("token") String token) {
        boolean isValid = tokenProvider.validateToken(token);
        if (isValid) {
            String username = tokenProvider.getUsernameFromToken(token);
            List<String> roles = tokenProvider.getRolesFromToken(token);
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .username(username)
                    .roles(roles)
                    .build());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
    }
}
