package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Any interview question: multiple choice, coding problem or free-text interview question. */
@Entity
@Table(name = "interview_questions")
@Getter
@Setter
@NoArgsConstructor
public class InterviewQuestion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false, length = 30)
    private InterviewRound round;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 10)
    private InterviewLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private InterviewQuestionType type;

    @Column(length = 100)
    private String topic;

    /** Title of a coding problem. */
    @Column(length = 200)
    private String title;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String text;

    /** Hint shown to the candidate while answering (for example "use the STAR method"). */
    @Column(length = 500)
    private String guidance;

    /** MCQ explanation, or the model answer of an interview question. Only revealed after submission. */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "question")
    @OrderBy("displayOrder ASC")
    private List<InterviewOption> options = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "interview_question_roles",
        joinColumns = @JoinColumn(name = "question_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<InterviewRole> roles = new LinkedHashSet<>();
}
