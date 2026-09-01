package com.autonomousreview.client;

import com.autonomousreview.dto.github.GitHubRepoApiResponse;
import com.autonomousreview.dto.github.GitHubUserApiResponse;
import com.autonomousreview.exception.GitHubApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GitHubClient {

    private final RestClient restClient;
    private final String githubApiBaseUrl;

    public GitHubClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.github.api-url:https://api.github.com}") String githubApiBaseUrl
    ) {
        this.githubApiBaseUrl = githubApiBaseUrl;
        this.restClient = restClientBuilder
                .baseUrl(githubApiBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public GitHubUserApiResponse getCurrentUser(String accessToken) {
        try {
            return restClient.get()
                    .uri("/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new GitHubApiException("GitHub authorization failed with status: " + response.getStatusCode());
                    })
                    .body(GitHubUserApiResponse.class);
        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException("Failed to communicate with GitHub API: " + e.getMessage(), e);
        }
    }

    public List<GitHubRepoApiResponse> getUserRepositories(String accessToken) {
        try {
            return restClient.get()
                    .uri("/user/repos?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new GitHubApiException("Failed to fetch repositories from GitHub. Status: " + response.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<List<GitHubRepoApiResponse>>() {});
        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException("Failed to retrieve repositories from GitHub API: " + e.getMessage(), e);
        }
    }
}
