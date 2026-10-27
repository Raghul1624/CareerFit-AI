package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_attempts")
@Getter
@Setter
@NoArgsConstructor
public class TestAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mock_test_id", nullable = false)
    private MockTest mockTest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    // Rules are snapshotted so later admin edits never change an existing attempt.
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "marks_per_question", nullable = false)
    private double marksPerQuestion;

    @Column(name = "negative_marks", nullable = false)
    private double negativeMarks;

    @Column(name = "passing_percentage", nullable = false)
    private double passingPercentage;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "max_score", nullable = false)
    private double maxScore;

    private Double score;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "incorrect_count")
    private Integer incorrectCount;

    @Column(name = "unanswered_count")
    private Integer unansweredCount;

    private Double percentage;

    private Boolean passed;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seqNo ASC")
    private List<TestAttemptQuestion> items = new ArrayList<>();
}
