package com.autonomousreview.controller;

import com.autonomousreview.client.GitHubClient;
import com.autonomousreview.config.TestRepositoryMockConfig;
import com.autonomousreview.dto.github.ConnectGitHubRequest;
import com.autonomousreview.dto.github.GitHubRepoApiResponse;
import com.autonomousreview.dto.github.GitHubUserApiResponse;
import com.autonomousreview.exception.GitHubApiException;
import com.autonomousreview.model.AuthProvider;
import com.autonomousreview.model.GitHubConnection;
import com.autonomousreview.model.Repository;
import com.autonomousreview.model.Role;
import com.autonomousreview.model.User;
import com.autonomousreview.repository.GitHubConnectionRepository;
import com.autonomousreview.repository.RepositoryEntityRepository;
import com.autonomousreview.repository.UserRepository;
import com.autonomousreview.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRepositoryMockConfig.class)
class GitHubControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GitHubConnectionRepository connectionRepository;

    @Autowired
    private RepositoryEntityRepository repositoryRepository;

    @Autowired
    private GitHubClient gitHubClient;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testUser = new User("dev@example.com", "hashed-password", "Dev User", Role.ROLE_USER, AuthProvider.LOCAL);
        testUser.setId("user-uuid-123");

        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(testUser));
        jwtToken = jwtService.generateToken(testUser);
    }

    @Test
    @DisplayName("POST /api/v1/github/connect successfully connects valid GitHub token")
    void testConnectGitHubSuccess() throws Exception {
        GitHubUserApiResponse ghUser = new GitHubUserApiResponse(999L, "octocat", "https://github.com/octocat.png", "The Octocat", "octocat@github.com");
        when(gitHubClient.getCurrentUser("ghp_validToken123")).thenReturn(ghUser);

        GitHubConnection savedConnection = new GitHubConnection("user-uuid-123", 999L, "octocat", "ghp_validToken123", "https://github.com/octocat.png", List.of("repo"));
        savedConnection.setId("conn-uuid-1");
        when(connectionRepository.findByUserId("user-uuid-123")).thenReturn(Optional.empty());
        when(connectionRepository.save(any(GitHubConnection.class))).thenReturn(savedConnection);

        GitHubRepoApiResponse repo1 = new GitHubRepoApiResponse(101L, "demo-repo", "octocat/demo-repo", "https://github.com/octocat/demo-repo", "main", false, new GitHubRepoApiResponse.GitHubOwnerApiResponse("octocat", "https://github.com/octocat.png"));
        when(gitHubClient.getUserRepositories("ghp_validToken123")).thenReturn(List.of(repo1));

        Repository savedRepo = new Repository("user-uuid-123", 101L, "demo-repo", "octocat/demo-repo", "octocat", "https://github.com/octocat/demo-repo", "main", false);
        when(repositoryRepository.findByUserIdAndGithubRepoId("user-uuid-123", 101L)).thenReturn(Optional.empty());
        when(repositoryRepository.save(any(Repository.class))).thenReturn(savedRepo);

        ConnectGitHubRequest request = new ConnectGitHubRequest("ghp_validToken123");

        mockMvc.perform(post("/api/v1/github/connect")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubUsername").value("octocat"))
                .andExpect(jsonPath("$.githubUserId").value(999));
    }

    @Test
    @DisplayName("POST /api/v1/github/connect fails with 400 for invalid token (Negative Test)")
    void testConnectGitHubInvalidToken() throws Exception {
        when(gitHubClient.getCurrentUser("invalid_token")).thenThrow(new GitHubApiException("GitHub authorization failed with status: 401 UNAUTHORIZED"));

        ConnectGitHubRequest request = new ConnectGitHubRequest("invalid_token");

        mockMvc.perform(post("/api/v1/github/connect")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GitHub API Error"))
                .andExpect(jsonPath("$.message").value("GitHub authorization failed with status: 401 UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/v1/github/connection returns 200 with connection details for connected user")
    void testGetConnectionSuccess() throws Exception {
        GitHubConnection conn = new GitHubConnection("user-uuid-123", 999L, "octocat", "ghp_tok", "https://avatar.png", List.of("repo"));
        conn.setId("conn-123");
        when(connectionRepository.findByUserId("user-uuid-123")).thenReturn(Optional.of(conn));

        mockMvc.perform(get("/api/v1/github/connection")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubUsername").value("octocat"))
                .andExpect(jsonPath("$.githubUserId").value(999));
    }

    @Test
    @DisplayName("GET /api/v1/github/connection returns 404 when user has not connected GitHub (Negative Test)")
    void testGetConnectionNotFound() throws Exception {
        when(connectionRepository.findByUserId("user-uuid-123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/github/connection")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No connected GitHub account found for user"));
    }

    @Test
    @DisplayName("GET /api/v1/github/repositories returns list of synced repositories")
    void testGetRepositories() throws Exception {
        GitHubConnection conn = new GitHubConnection("user-uuid-123", 999L, "octocat", "ghp_tok", "https://avatar.png", List.of("repo"));
        when(connectionRepository.findByUserId("user-uuid-123")).thenReturn(Optional.of(conn));

        GitHubRepoApiResponse repoApi = new GitHubRepoApiResponse(101L, "demo-repo", "octocat/demo-repo", "https://github.com/octocat/demo-repo", "main", false, new GitHubRepoApiResponse.GitHubOwnerApiResponse("octocat", "https://avatar.png"));
        when(gitHubClient.getUserRepositories("ghp_tok")).thenReturn(List.of(repoApi));

        Repository repo = new Repository("user-uuid-123", 101L, "demo-repo", "octocat/demo-repo", "octocat", "https://github.com/octocat/demo-repo", "main", false);
        repo.setId("repo-doc-id");
        when(repositoryRepository.findByUserIdAndGithubRepoId("user-uuid-123", 101L)).thenReturn(Optional.of(repo));
        when(repositoryRepository.save(any(Repository.class))).thenReturn(repo);

        mockMvc.perform(get("/api/v1/github/repositories")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].githubRepoId").value(101))
                .andExpect(jsonPath("$[0].name").value("demo-repo"))
                .andExpect(jsonPath("$[0].fullName").value("octocat/demo-repo"));
    }

    @Test
    @DisplayName("POST /api/v1/github/repositories/{repoId}/enable marks repository enabledForReview")
    void testEnableRepositoryForReview() throws Exception {
        Repository repo = new Repository("user-uuid-123", 101L, "demo-repo", "octocat/demo-repo", "octocat", "https://github.com/octocat/demo-repo", "main", false);
        repo.setId("repo-doc-id");
        when(repositoryRepository.findByUserIdAndGithubRepoId("user-uuid-123", 101L)).thenReturn(Optional.of(repo));

        Repository enabledRepo = new Repository("user-uuid-123", 101L, "demo-repo", "octocat/demo-repo", "octocat", "https://github.com/octocat/demo-repo", "main", false);
        enabledRepo.setId("repo-doc-id");
        enabledRepo.setEnabledForReview(true);
        when(repositoryRepository.save(any(Repository.class))).thenReturn(enabledRepo);

        mockMvc.perform(post("/api/v1/github/repositories/101/enable")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubRepoId").value(101))
                .andExpect(jsonPath("$.enabledForReview").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/github/repositories without token returns 403 Forbidden (Negative Test)")
    void testUnauthenticatedAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/github/repositories"))
                .andExpect(status().isForbidden());
    }
}
