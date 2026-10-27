package com.careerfit.repository;

import com.careerfit.entity.InterviewRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewRoleRepository extends JpaRepository<InterviewRole, Long> {
    List<InterviewRole> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<InterviewRole> findByCodeAndActiveTrue(String code);
}
