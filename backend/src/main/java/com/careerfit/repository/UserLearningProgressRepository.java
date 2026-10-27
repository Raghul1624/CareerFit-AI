package com.careerfit.repository;

import com.careerfit.entity.UserLearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLearningProgressRepository extends JpaRepository<UserLearningProgress, Long> {
    Optional<UserLearningProgress> findByUserIdAndItemId(Long userId, Long itemId);
    List<UserLearningProgress> findByUserIdAndCompletedTrue(Long userId);
}
