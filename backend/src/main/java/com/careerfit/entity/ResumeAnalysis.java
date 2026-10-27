package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "resume_analyses")
@Getter
@Setter
@NoArgsConstructor
public class ResumeAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_path_id")
    private CareerPath careerPath;

    @Column(name = "keyword_coverage")
    private Double keywordCoverage;

    /** JSON arrays of strings. The resume text itself is never stored. */
    @Column(name = "extracted_skills", columnDefinition = "TEXT")
    private String extractedSkills;

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
