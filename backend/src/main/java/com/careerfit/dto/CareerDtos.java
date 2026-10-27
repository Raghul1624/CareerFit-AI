package com.careerfit.dto;

import java.util.List;

public final class CareerDtos {
    private CareerDtos() {}

    public record CareerPathSummary(Long id, String name, String description, Long categoryId, String categoryName) {}

    public record SkillDto(Long id, String name, String description) {}

    /** Reference to the curated roadmap of a path, when one exists. */
    public record RoadmapRef(Long id, String title, int steps) {}

    /**
     * Career path page data.
     * <ul>
     *   <li>{@code careerMockTest}: the path's own mock test (null only if none is configured)</li>
     *   <li>{@code recommendedTests}: that test first, then the skill tests for the path's skills</li>
     *   <li>{@code learningTopics}: the topics the question bank covers for each required skill</li>
     *   <li>{@code roadmap}: curated roadmap, or null; {@code suggestedLearningOrder} is always provided</li>
     * </ul>
     */
    public record CareerPathDetail(Long id, String name, String description, String categoryName,
                                   List<SkillDto> skills, List<TestDtos.MockTestSummary> recommendedTests,
                                   TestDtos.MockTestSummary careerMockTest,
                                   List<TestDtos.SkillCoverage> learningTopics,
                                   RoadmapRef roadmap, List<String> suggestedLearningOrder) {}
}
