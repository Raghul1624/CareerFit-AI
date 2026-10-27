package com.careerfit.repository;

import com.careerfit.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {
    Optional<ResumeAnalysis> findFirstByResumeIdOrderByCreatedAtDesc(Long resumeId);
}
