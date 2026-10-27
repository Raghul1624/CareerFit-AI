package com.careerfit.dto;

import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record SkillPerformance(String skill, long correct, long total, double accuracy) {}

    public record Summary(String fullName, String targetCareer, long testsCompleted,
                          Double averageScore, Double bestScore,
                          List<TestDtos.AttemptSummary> recentAttempts,
                          List<SkillPerformance> skillPerformance,
                          List<String> recommendedTopics) {}
}
