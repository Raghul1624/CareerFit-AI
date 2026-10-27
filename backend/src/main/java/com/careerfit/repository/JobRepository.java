package com.careerfit.repository;

import com.careerfit.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findByActiveTrue();
    Optional<Job> findByIdAndActiveTrue(Long id);
}
