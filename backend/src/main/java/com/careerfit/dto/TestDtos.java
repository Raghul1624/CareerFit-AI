package com.careerfit.dto;

import com.careerfit.entity.AttemptStatus;
import com.careerfit.entity.Difficulty;
import com.careerfit.entity.QuestionType;
import com.careerfit.entity.TestMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class TestDtos {
    private TestDtos() {}

    /** Questions available for one skill of a test, with the topics they cover. */
    public record SkillCoverage(Long skillId, String skillName, int questionCount, List<String> topics) {}

    /**
     * Everything the test list, the career page and the instructions screen need.
     * {@code questionCount} is the number of questions an attempt will contain;
     * {@code availableQuestions} is the size of the eligible pool it is drawn from.
     * A test that cannot be served is reported with {@code startable = false} and a reason
     * instead of silently being shortened or padded with unrelated questions.
     */
    public record MockTestSummary(Long id, String title, String description, TestMode mode,
                                  Long careerPathId, String careerPathName, List<String> skills,
                                  int durationMinutes, int questionCount, double marksPerQuestion,
                                  double totalMarks, double negativeMarks, double passingPercentage,
                                  Integer maxAttempts,
                                  List<Long> skillIds, List<SkillCoverage> coverage, List<String> topics,
                                  List<Difficulty> difficulties, int availableQuestions,
                                  boolean startable, String unavailableReason) {}

    // ---- Active attempt (PUBLIC data only: no correct flags, no explanations) ----
    public record PublicOption(Long id, String text) {}

    public record PublicQuestion(Long id, int seqNo, String text, QuestionType type,
                                 List<PublicOption> options, List<Long> selectedOptionIds,
                                 boolean markedForReview) {}

    public record AttemptView(Long id, Long testId, String title, AttemptStatus status,
                              Instant startedAt, Instant expiresAt, Instant serverTime,
                              int durationMinutes, int totalQuestions, List<PublicQuestion> questions,
                              TestMode mode, Long careerPathId, String careerPathName) {}

    public record AnswerItem(@NotNull Long questionId, @Size(max = 10) List<Long> selectedOptionIds,
                             boolean markedForReview) {}

    public record SaveAnswersRequest(@NotNull @Size(max = 200) List<@Valid AnswerItem> answers) {}

    // ---- History / results ----
    public record AttemptSummary(Long id, Long testId, String testTitle, AttemptStatus status,
                                 Instant startedAt, Instant submittedAt, Double score, double maxScore,
                                 Double percentage, Boolean passed, Long timeTakenSeconds, int totalQuestions,
                                 TestMode mode, Long careerPathId, String careerPathName) {}

    public record TopicStat(String topic, int correct, int total, double accuracy) {}

    public record ReviewOption(Long id, String text, boolean correct, boolean selected) {}

    public record ReviewItem(Long questionId, int seqNo, String text, String topic, QuestionType type,
                             List<ReviewOption> options, Boolean correct, boolean answered,
                             String explanation, Double awardedMarks) {}

    public record ResultView(Long id, Long testId, String testTitle, AttemptStatus status,
                             Instant startedAt, Instant submittedAt, double score, double maxScore,
                             double percentage, boolean passed, double passingPercentage,
                             long timeTakenSeconds, int totalQuestions, int correctCount,
                             int incorrectCount, int unansweredCount, List<TopicStat> topicPerformance,
                             List<ReviewItem> review, List<String> revisionTopics, boolean canRetake,
                             TestMode mode, Long careerPathId, String careerPathName) {}
}
