package com.impactflow.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long testExpiration = 60000; // 1 minute

    @BeforeEach
    public void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", testExpiration);
    }

    @Test
    public void testGenerateAndValidateToken() {
        String username = "testuser";
        List<String> roles = Collections.singletonList("ROLE_DEVELOPER");

        String token = jwtTokenProvider.generateToken(username, roles);
        assertNotNull(token);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(username, jwtTokenProvider.getUsernameFromToken(token));
        
        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);
        assertEquals(1, extractedRoles.size());
        assertEquals("ROLE_DEVELOPER", extractedRoles.get(0));
    }

    @Test
    public void testInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalid.token";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }
}
