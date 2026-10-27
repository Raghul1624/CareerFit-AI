package com.careerfit.dto;

import com.careerfit.entity.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class JobDtos {
    private JobDtos() {}

    public record JobFilter(String q, String location, WorkMode workMode, ExperienceLevel experienceLevel,
                            EmploymentType employmentType, Long careerPathId) {}

    public record MatchInfo(double percent, List<String> matchedSkills, List<String> missingRequired,
                            List<String> missingPreferred) {}

    public record JobSummary(Long id, String title, String company, String location, WorkMode workMode,
                             ExperienceLevel experienceLevel, EmploymentType employmentType, Instant postedAt,
                             boolean sample, List<String> requiredSkills, Double matchPercent, boolean saved) {}

    public record ApplicationDto(Long jobId, String jobTitle, String company, ApplicationStatus status,
                                 String notes, Instant updatedAt) {}

    public record JobDetail(Long id, String title, String company, String description, String location,
                            WorkMode workMode, ExperienceLevel experienceLevel, EmploymentType employmentType,
                            String applicationUrl, Instant postedAt, boolean sample,
                            List<String> requiredSkills, List<String> preferredSkills,
                            MatchInfo match, boolean saved, ApplicationDto application) {}

    public record ApplicationRequest(@NotNull ApplicationStatus status, @Size(max = 500) String notes) {}
}
