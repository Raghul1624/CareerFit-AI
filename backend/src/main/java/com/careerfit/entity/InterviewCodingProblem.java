package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Extra data of a CODING question (the statement itself is the question text). */
@Entity
@Table(name = "interview_coding_problems")
@Getter
@Setter
@NoArgsConstructor
public class InterviewCodingProblem {
    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "constraints_text", columnDefinition = "TEXT")
    private String constraints;

    @Column(name = "input_format", columnDefinition = "TEXT")
    private String inputFormat;

    @Column(name = "output_format", columnDefinition = "TEXT")
    private String outputFormat;

    @Column(name = "time_limit_seconds", nullable = false)
    private double timeLimitSeconds = 2;

    @Column(name = "memory_limit_mb", nullable = false)
    private int memoryLimitMb = 256;
}
