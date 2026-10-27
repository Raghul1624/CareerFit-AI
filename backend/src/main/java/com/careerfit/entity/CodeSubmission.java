package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A graded code submission (all sample and hidden test cases were run by the sandbox). */
@Entity
@Table(name = "code_submissions")
@Getter
@Setter
@NoArgsConstructor
public class CodeSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_item_id", nullable = false)
    private InterviewAttemptItem attemptItem;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CodeLanguage language;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CodeVerdict status;

    @Column(name = "passed_tests", nullable = false)
    private int passedTests;

    @Column(name = "total_tests", nullable = false)
    private int totalTests;

    @Column(name = "score_percent", nullable = false)
    private double scorePercent;

    @Column(name = "error_output", columnDefinition = "TEXT")
    private String errorOutput;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    /** Per-test verdicts; hidden tests carry no input or expected output. */
    @Column(name = "results_json", columnDefinition = "TEXT")
    private String resultsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
