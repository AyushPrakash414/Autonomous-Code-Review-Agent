package com.autonomousreview.config;

import com.autonomousreview.client.GitHubClient;
import com.autonomousreview.repository.GitHubConnectionRepository;
import com.autonomousreview.repository.RepositoryEntityRepository;
import com.autonomousreview.repository.UserRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestRepositoryMockConfig {

    @Bean
    @Primary
    public UserRepository userRepository() {
        return Mockito.mock(UserRepository.class);
    }

    @Bean
    @Primary
    public GitHubConnectionRepository gitHubConnectionRepository() {
        return Mockito.mock(GitHubConnectionRepository.class);
    }

    @Bean
    @Primary
    public RepositoryEntityRepository repositoryEntityRepository() {
        return Mockito.mock(RepositoryEntityRepository.class);
    }

    @Bean
    @Primary
    public GitHubClient gitHubClient() {
        return Mockito.mock(GitHubClient.class);
    }
}
