package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_attempt_items")
@Getter
@Setter
@NoArgsConstructor
public class InterviewAttemptItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private InterviewAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    @Column(name = "seq_no", nullable = false)
    private int seqNo;

    /** Comma-separated option ids chosen for a multiple-choice question. */
    @Column(name = "selected_option_ids", length = 255)
    private String selectedOptionIds;

    /** The text answer, or the draft source code of a coding problem. */
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(length = 20)
    private String language;

    @Column(name = "marked_for_review", nullable = false)
    private boolean markedForReview;

    private Boolean correct;

    @Column(name = "awarded_score")
    private Double awardedScore;

    @Column(name = "max_score", nullable = false)
    private double maxScore;

    /** JSON with the rubric breakdown of a text answer. */
    @Column(name = "evaluation_json", columnDefinition = "TEXT")
    private String evaluationJson;
}
