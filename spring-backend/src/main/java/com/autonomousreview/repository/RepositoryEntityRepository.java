package com.autonomousreview.repository;

import com.autonomousreview.model.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryEntityRepository extends MongoRepository<Repository, String> {
    List<Repository> findByUserId(String userId);
    List<Repository> findByUserIdAndEnabledForReviewTrue(String userId);
    Optional<Repository> findByUserIdAndGithubRepoId(String userId, Long githubRepoId);
    Optional<Repository> findByFullName(String fullName);
    Optional<Repository> findByUserIdAndFullName(String userId, String fullName);
}
