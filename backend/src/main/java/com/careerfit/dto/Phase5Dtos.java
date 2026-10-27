package com.careerfit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** DTOs for learning roadmaps, skills, analytics and skill-gap analysis. */
public final class Phase5Dtos {
    private Phase5Dtos() {}

    public record TestRef(Long id, String title) {}

    // ---- learning roadmap
    public record RoadmapSummary(Long id, Long careerPathId, String careerPathName, String title,
                                 String description, int totalSteps, int completedSteps) {}

    public record RoadmapItemDto(Long id, int stepOrder, String title, String description,
                                 String estimatedDuration, List<String> skills, boolean completed,
                                 Instant completedAt, List<TestRef> recommendedTests) {}

    public record RoadmapDetail(Long id, Long careerPathId, String careerPathName, String title,
                                String description, int totalSteps, int completedSteps,
                                double progressPercent, List<RoadmapItemDto> items) {}

    public record ProgressRequest(@NotNull Boolean completed) {}

    public record ProgressResult(Long itemId, boolean completed, Instant completedAt) {}

    // ---- skills
    public record SetSkillsRequest(@NotNull @Size(max = 100) List<Long> skillIds) {}

    // ---- analytics
    public record TrendPoint(Long attemptId, String testTitle, Instant date, double percentage) {}

    public record PerformanceView(List<TrendPoint> trend, List<DashboardDtos.SkillPerformance> skills) {}

    public record GapItem(Long skillId, String skill, String status, Double accuracy, boolean declared,
                          String reason, List<TestRef> recommendedTests) {}

    public record SkillGap(Long careerPathId, String targetCareer, List<GapItem> items, List<String> suggestedOrder) {}
}
