package com.careerfit.repository;

import com.careerfit.entity.LearningRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LearningRoadmapRepository extends JpaRepository<LearningRoadmap, Long> {

    @Query("select r from LearningRoadmap r join fetch r.careerPath c order by c.name")
    List<LearningRoadmap> findAllWithCareerPath();

    Optional<LearningRoadmap> findByCareerPathId(Long careerPathId);
}
