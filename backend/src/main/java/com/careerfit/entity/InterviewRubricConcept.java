package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One idea a good answer should contain, with the keywords that indicate it. */
@Entity
@Table(name = "interview_rubric_concepts")
@Getter
@Setter
@NoArgsConstructor
public class InterviewRubricConcept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false, length = 100)
    private String name;

    /** Comma-separated keywords, matched as lower-case substrings. */
    @Column(nullable = false, length = 500)
    private String keywords;

    @Column(nullable = false)
    private int weight = 1;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
