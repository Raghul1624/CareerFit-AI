package com.careerfit.repository;

import com.careerfit.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    Optional<JobApplication> findByUserIdAndJobId(Long userId, Long jobId);
    List<JobApplication> findByUserIdOrderByUpdatedAtDesc(Long userId);
    long deleteByUserIdAndJobId(Long userId, Long jobId);
}
