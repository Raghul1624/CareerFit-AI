package com.careerfit.repository;

import com.careerfit.entity.LearningRoadmapItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningItemRepository extends JpaRepository<LearningRoadmapItem, Long> {
}
