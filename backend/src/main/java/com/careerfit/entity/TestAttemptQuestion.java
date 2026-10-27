package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_attempt_questions")
@Getter
@Setter
@NoArgsConstructor
public class TestAttemptQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "seq_no", nullable = false)
    private int seqNo;

    /** Comma-separated option ids chosen by the user, or null when unanswered. */
    @Column(name = "selected_option_ids", length = 255)
    private String selectedOptionIds;

    @Column(name = "marked_for_review", nullable = false)
    private boolean markedForReview;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "awarded_marks")
    private Double awardedMarks;
}
