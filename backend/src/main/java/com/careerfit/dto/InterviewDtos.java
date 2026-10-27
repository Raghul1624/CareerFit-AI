package com.careerfit.dto;

import com.careerfit.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request and response shapes of the Interview Preparation API. */
public final class InterviewDtos {

    private InterviewDtos() {
    }

    // ---------------------------------------------------------------- catalog
    /** One difficulty level of one round for one role. An assessment is only {@code available} when its questions exist. */
    public record LevelAvailability(InterviewLevel level, boolean available, int questionCount, int durationMinutes,
                                    double passingPercentage, int availableQuestions, String reason) {}

    public record RoundInfo(InterviewRound round, String label, String description, List<LevelAvailability> levels) {}

    public record RoleInfo(Long id, String code, String name, String description, List<RoundInfo> rounds) {}

    public record InterviewOverview(long totalAttempts, List<HistoryItem> recent) {}

    // ---------------------------------------------------------------- starting and answering
    public record StartRequest(@NotBlank @Size(max = 30) String roleCode, @NotNull InterviewRound round, @NotNull InterviewLevel level) {}

    public record OptionView(Long id, String text) {}

    public record SampleCase(String input, String expectedOutput) {}

    public record CodingView(String constraints, String inputFormat, String outputFormat, List<SampleCase> samples,
                             int hiddenTestCount, double timeLimitSeconds, List<CodeLanguage> languages,
                             Map<CodeLanguage, String> starterCode, Double bestScorePercent, int submissionCount) {}

    /** What the candidate has saved so far for a question. */
    public record SavedAnswer(List<Long> selectedOptionIds, String answerText, String language, boolean markedForReview) {}

    /** A question as shown DURING the attempt: never contains the answer key, the model answer, the rubric or hidden tests. */
    public record QuestionView(Long id, int seqNo, InterviewQuestionType type, String topic, String title, String text,
                               String guidance, List<OptionView> options, CodingView coding, SavedAnswer saved) {}

    public record AttemptView(Long id, String roleCode, String roleName, InterviewRound round, String roundLabel,
                              InterviewLevel level, AttemptStatus status, Instant startedAt, Instant expiresAt,
                              Instant serverTime, int durationMinutes, int totalQuestions, double passingPercentage,
                              List<QuestionView> questions) {}

    public record AnswerInput(@NotNull Long questionId, @Size(max = 10) List<Long> selectedOptionIds,
                              @Size(max = 10000) String answerText, @Size(max = 20) String language, Boolean markedForReview) {}

    public record SaveAnswersRequest(@NotNull @Size(max = 50) List<@Valid AnswerInput> answers) {}

    // ---------------------------------------------------------------- coding
    public record RunRequest(@NotNull CodeLanguage language, @NotBlank @Size(max = 10000) String sourceCode,
                             @Size(max = 4000) String stdin) {}

    public record SubmitCodeRequest(@NotNull CodeLanguage language, @NotBlank @Size(max = 10000) String sourceCode) {}

    /** Hidden test cases only carry index, verdict and time. */
    public record CaseResultView(int index, boolean hidden, String verdict, String input, String expectedOutput,
                                 String actualOutput, String errorOutput, Integer timeMs) {}

    public record RunResultView(boolean custom, String status, String stdout, String stderr, String compileOutput,
                                Integer timeMs, List<CaseResultView> cases, Integer passed, Integer total) {}

    public record SubmissionView(Long id, CodeLanguage language, CodeVerdict verdict, int passedTests, int totalTests,
                                 double scorePercent, String compileOutput, Integer executionTimeMs, Instant submittedAt,
                                 List<CaseResultView> cases, String sourceCode) {}

    public record CodingStatus(boolean configured, List<CodeLanguage> languages, String message) {}

    // ---------------------------------------------------------------- results
    public record DimensionView(String name, int score, int weightPercent, String comment) {}

    public record EvaluationView(int overall, List<DimensionView> dimensions, List<String> strengths, List<String> improvements,
                                 List<String> matchedConcepts, List<String> missedConcepts, int wordCount) {}

    public record ResultOption(Long id, String text, boolean correct, boolean selected) {}

    public record CodingResult(Double bestScorePercent, int submissionCount, CodeVerdict lastVerdict, Integer lastPassed,
                               Integer lastTotal, CodeLanguage lastLanguage, String lastSourceCode) {}

    /** One question after submission: the answer key, explanation or model answer and the evaluation are revealed here. */
    public record ResultItem(Long questionId, int seqNo, InterviewQuestionType type, String topic, String title, String text,
                             List<ResultOption> options, String yourAnswer, String language, String explanation,
                             Boolean correct, double score, double maxScore, EvaluationView evaluation, CodingResult coding) {}

    public record TopicPerformance(String topic, int total, int correct) {}

    public record ResultView(Long attemptId, String roleCode, String roleName, InterviewRound round, String roundLabel,
                             InterviewLevel level, AttemptStatus status, Instant startedAt, Instant submittedAt,
                             Long timeTakenSeconds, double score, double maxScore, double percentage, boolean passed,
                             double passingPercentage, int totalQuestions, int correctCount, String summaryFeedback,
                             String scoringMethod, List<TopicPerformance> topicPerformance, List<ResultItem> items) {}

    public record HistoryItem(Long id, String roleCode, String roleName, InterviewRound round, String roundLabel,
                              InterviewLevel level, AttemptStatus status, Double score, double maxScore, Double percentage,
                              Boolean passed, Instant startedAt, Instant submittedAt, int totalQuestions) {}
}
