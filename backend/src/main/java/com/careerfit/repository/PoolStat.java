package com.careerfit.repository;

import com.careerfit.entity.Difficulty;

/** Number of active questions for one (skill, topic, difficulty) combination. */
public record PoolStat(Long skillId, String topic, Difficulty difficulty, Long count) {
}
