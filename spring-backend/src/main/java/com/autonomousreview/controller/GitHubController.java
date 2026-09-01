package com.autonomousreview.controller;

import com.autonomousreview.dto.github.ConnectGitHubRequest;
import com.autonomousreview.dto.github.GitHubConnectionDto;
import com.autonomousreview.dto.github.GitHubRepoDto;
import com.autonomousreview.model.User;
import com.autonomousreview.service.GitHubService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @PostMapping("/connect")
    public ResponseEntity<GitHubConnectionDto> connectGitHub(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ConnectGitHubRequest request
    ) {
        GitHubConnectionDto response = gitHubService.connectAccount(user.getId(), request.accessToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/connection")
    public ResponseEntity<GitHubConnectionDto> getConnection(
            @AuthenticationPrincipal User user
    ) {
        GitHubConnectionDto connection = gitHubService.getConnection(user.getId());
        return ResponseEntity.ok(connection);
    }

    @GetMapping("/repositories")
    public ResponseEntity<List<GitHubRepoDto>> getRepositories(
            @AuthenticationPrincipal User user
    ) {
        List<GitHubRepoDto> repositories = gitHubService.getUserRepositories(user.getId());
        return ResponseEntity.ok(repositories);
    }

    @PostMapping("/repositories/{repoId}/enable")
    public ResponseEntity<GitHubRepoDto> enableRepository(
            @AuthenticationPrincipal User user,
            @PathVariable Long repoId
    ) {
        GitHubRepoDto enabledRepo = gitHubService.enableRepository(user.getId(), repoId);
        return ResponseEntity.ok(enabledRepo);
    }

    @GetMapping("/repositories/enabled")
    public ResponseEntity<List<GitHubRepoDto>> getEnabledRepositories(
            @AuthenticationPrincipal User user
    ) {
        List<GitHubRepoDto> repositories = gitHubService.getEnabledRepositories(user.getId());
        return ResponseEntity.ok(repositories);
    }
}
