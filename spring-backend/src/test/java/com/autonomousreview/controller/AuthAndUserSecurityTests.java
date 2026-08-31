package com.autonomousreview.controller;

import com.autonomousreview.dto.auth.LoginRequest;
import com.autonomousreview.dto.auth.RegisterRequest;
import com.autonomousreview.model.AuthProvider;
import com.autonomousreview.model.Role;
import com.autonomousreview.model.User;
import com.autonomousreview.repository.UserRepository;
import com.autonomousreview.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndUserSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    private User existingUser;
    private String validJwtToken;
    private String expiredJwtToken;

    @BeforeEach
    void setUp() {
        existingUser = new User(
                "alice@example.com",
                passwordEncoder.encode("SecretPass123"),
                "Alice Smith",
                Role.ROLE_USER,
                AuthProvider.LOCAL
        );
        existingUser.setId("user-123-uuid");

        validJwtToken = jwtService.generateToken(existingUser);
        expiredJwtToken = jwtService.generateTokenWithExpiration(existingUser, -5000L);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register creates user and returns JWT (Valid Registration)")
    void testValidRegistration() throws Exception {
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        User newUser = new User("bob@example.com", passwordEncoder.encode("BobPassword1"), "Bob Builder", Role.ROLE_USER, AuthProvider.LOCAL);
        newUser.setId("user-bob-id");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        RegisterRequest request = new RegisterRequest("bob@example.com", "BobPassword1", "Bob Builder");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("bob@example.com"))
                .andExpect(jsonPath("$.user.fullName").value("Bob Builder"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register returns 409 Conflict when email exists (Negative Test)")
    void testDuplicateRegistration() throws Exception {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("alice@example.com", "Password123", "Alice Clone");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User with email 'alice@example.com' already exists"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 with JWT for valid credentials")
    void testValidLogin() throws Exception {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));

        LoginRequest request = new LoginRequest("alice@example.com", "SecretPass123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 401 Unauthorized for invalid password (Negative Test)")
    void testInvalidLogin() throws Exception {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));

        LoginRequest request = new LoginRequest("alice@example.com", "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me without token returns 403 Forbidden (Unauthorized endpoint)")
    void testUnauthorizedEndpointAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/users/me with valid Bearer token returns 200 with user profile")
    void testAuthorizedEndpointAccess() throws Exception {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Smith"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me with expired token returns 403 Forbidden (Negative Test)")
    void testExpiredTokenAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredJwtToken))
                .andExpect(status().isForbidden());
    }
}
