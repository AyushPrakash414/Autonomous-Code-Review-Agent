package com.autonomousreview.security;

import com.autonomousreview.model.AuthProvider;
import com.autonomousreview.model.Role;
import com.autonomousreview.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTests {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L); // 1 hour

        testUser = new User("developer@example.com", "encoded-pwd", "Lead Developer", Role.ROLE_USER, AuthProvider.LOCAL);
    }

    @Test
    @DisplayName("generateToken produces valid non-empty JWT with correct subject")
    void testGenerateToken() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);
        assertFalse(token.isBlank());

        String username = jwtService.extractUsername(token);
        assertEquals("developer@example.com", username);
        assertTrue(jwtService.isTokenValid(token, testUser));
    }

    @Test
    @DisplayName("isTokenValid returns false when username does not match")
    void testTokenInvalidForDifferentUser() {
        String token = jwtService.generateToken(testUser);
        User differentUser = new User("other@example.com", "pwd", "Other User", Role.ROLE_USER, AuthProvider.LOCAL);

        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    @DisplayName("isTokenExpired returns true for expired token (Negative Test)")
    void testTokenExpiration() {
        // Generate token with 1ms expiration
        String expiredToken = jwtService.generateTokenWithExpiration(testUser, -1000L);
        assertTrue(jwtService.isTokenExpired(expiredToken));
        assertFalse(jwtService.isTokenValid(expiredToken, testUser));
    }

    @Test
    @DisplayName("isTokenValid returns false for tampered token (Negative Test)")
    void testTamperedToken() {
        String token = jwtService.generateToken(testUser);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(jwtService.isTokenValid(tamperedToken, testUser));
    }
}
