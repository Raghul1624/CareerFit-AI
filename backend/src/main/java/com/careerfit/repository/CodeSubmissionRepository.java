package com.careerfit.repository;

import com.careerfit.entity.CodeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {
    List<CodeSubmission> findByAttemptItemIdOrderByCreatedAtDescIdDesc(Long attemptItemId);

    Optional<CodeSubmission> findFirstByAttemptItemIdOrderByCreatedAtDescIdDesc(Long attemptItemId);

    long countByAttemptItemId(Long attemptItemId);

    @Query("select max(s.scorePercent) from CodeSubmission s where s.attemptItem.id = :itemId")
    Double findBestScore(@Param("itemId") Long itemId);
}
