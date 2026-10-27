package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "mock_tests")
@Getter
@Setter
@NoArgsConstructor
public class MockTest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_mode", nullable = false, length = 20)
    private TestMode mode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_path_id")
    private CareerPath careerPath;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "marks_per_question", nullable = false)
    private double marksPerQuestion;

    @Column(name = "negative_marks", nullable = false)
    private double negativeMarks;

    @Column(name = "passing_percentage", nullable = false)
    private double passingPercentage;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "mock_test_skills",
        joinColumns = @JoinColumn(name = "mock_test_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new LinkedHashSet<>();

    /**
     * The skills this test may draw questions from. A career test is restricted to skills that
     * really belong to its career path, so a misconfigured link can never pull unrelated questions
     * (for example Python questions) into a Java Developer test. A skill test uses its own skills.
     */
    public Set<Long> eligibleSkillIds() {
        Set<Long> ids = skills.stream().map(Skill::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (mode == TestMode.CAREER && careerPath != null) {
            Set<Long> pathSkillIds = careerPath.getSkills().stream().map(Skill::getId).collect(Collectors.toSet());
            ids.retainAll(pathSkillIds);
        }
        return ids;
    }
}
