package com.impactflow.auth.controller;

import com.impactflow.auth.domain.User;
import com.impactflow.auth.dto.AuthResponse;
import com.impactflow.auth.dto.LoginRequest;
import com.impactflow.auth.dto.RegisterRequest;
import com.impactflow.auth.repository.UserRepository;
import com.impactflow.auth.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtTokenProvider=jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request){

        if(userRepository.existsByUsername(request.getUsername())){
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user=new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        if(request.getRoles()==null||request.getRoles().isEmpty()){
            user.setRoles(List.of("ROLE_DEVELOPER"));
        }else{
            user.setRoles(request.getRoles());
        }

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request){

        User user=userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if(user==null||!passwordEncoder.matches(request.getPassword(),user.getPassword())){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }

        String token=jwtTokenProvider.generateToken(
                user.getUsername(),
                user.getRoles()
        );

        return ResponseEntity.ok(
                new AuthResponse(
                        token,
                        user.getUsername(),
                        user.getRoles()
                )
        );
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestParam String token){

        if(!jwtTokenProvider.validateToken(token)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token");
        }

        String username=jwtTokenProvider.getUsernameFromToken(token);
        List<String> roles=jwtTokenProvider.getRolesFromToken(token);

        return ResponseEntity.ok(
                new AuthResponse(
                        token,
                        username,
                        roles
                )
        );
    }
}