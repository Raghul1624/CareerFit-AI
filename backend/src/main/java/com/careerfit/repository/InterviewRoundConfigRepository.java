package com.careerfit.repository;

import com.careerfit.entity.InterviewLevel;
import com.careerfit.entity.InterviewRound;
import com.careerfit.entity.InterviewRoundConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewRoundConfigRepository extends JpaRepository<InterviewRoundConfig, Long> {
    Optional<InterviewRoundConfig> findByRoundAndLevel(InterviewRound round, InterviewLevel level);
}
