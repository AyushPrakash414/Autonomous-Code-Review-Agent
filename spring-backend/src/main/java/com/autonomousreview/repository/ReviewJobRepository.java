package com.autonomousreview.repository;

import com.autonomousreview.model.ReviewJob;
import com.autonomousreview.model.ReviewStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewJobRepository extends MongoRepository<ReviewJob, String> {
    Optional<ReviewJob> findByDeduplicationKey(String deduplicationKey);
    List<ReviewJob> findByRepoFullNameAndPullNumber(String repoFullName, Integer pullNumber);
    List<ReviewJob> findByStatus(ReviewStatus status);
    List<ReviewJob> findByRepositoryId(String repositoryId);
    boolean existsByDeduplicationKey(String deduplicationKey);
}
