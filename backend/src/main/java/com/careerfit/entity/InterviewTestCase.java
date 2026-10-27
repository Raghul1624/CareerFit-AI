package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Hidden cases are evaluated on the server and never sent to the browser. */
@Entity
@Table(name = "interview_test_cases")
@Getter
@Setter
@NoArgsConstructor
public class InterviewTestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "input_data", nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
