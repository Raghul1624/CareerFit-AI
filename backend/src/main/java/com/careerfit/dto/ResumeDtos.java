package com.careerfit.dto;

import java.time.Instant;
import java.util.List;

public final class ResumeDtos {
    private ResumeDtos() {}

    public static final String DISCLAIMER =
        "This is a keyword coverage score based on simple text matching. It is not an ATS score and does not predict whether you will be shortlisted.";

    public record ResumeDto(Long id, String filename, long sizeBytes, Instant uploadedAt,
                            Instant lastAnalyzedAt, Double lastCoverage) {}

    public record AnalysisDto(Long id, Instant createdAt, Long careerPathId, String careerPathName,
                              Double keywordCoverage, List<String> extractedSkills, List<String> missingSkills,
                              List<String> suggestions, int wordCount, int pageCount, String disclaimer,
                              List<String> aiFeedback) {}

    public record ResumeDetail(Long id, String filename, long sizeBytes, Instant uploadedAt, AnalysisDto latestAnalysis) {}

    /** careerPathId is optional; the user's target career is used when it is absent. */
    public record AnalyzeRequest(Long careerPathId) {}
}
