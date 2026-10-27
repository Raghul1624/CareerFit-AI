package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Size, duration and pass mark of one round at one difficulty level. */
@Entity
@Table(name = "interview_round_configs")
@Getter
@Setter
@NoArgsConstructor
public class InterviewRoundConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false, length = 30)
    private InterviewRound round;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 10)
    private InterviewLevel level;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "passing_percentage", nullable = false)
    private double passingPercentage;
}
