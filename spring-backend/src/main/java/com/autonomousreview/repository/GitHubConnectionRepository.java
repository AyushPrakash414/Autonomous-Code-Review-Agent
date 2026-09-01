package com.autonomousreview.repository;

import com.autonomousreview.model.GitHubConnection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GitHubConnectionRepository extends MongoRepository<GitHubConnection, String> {
    Optional<GitHubConnection> findByUserId(String userId);
    boolean existsByUserId(String userId);
    void deleteByUserId(String userId);
}
