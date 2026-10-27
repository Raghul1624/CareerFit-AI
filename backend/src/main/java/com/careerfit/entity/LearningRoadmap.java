package com.careerfit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "learning_roadmaps")
@Getter
@Setter
@NoArgsConstructor
public class LearningRoadmap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "career_path_id", nullable = false, unique = true)
    private CareerPath careerPath;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "roadmap")
    @OrderBy("stepOrder ASC")
    private List<LearningRoadmapItem> items = new ArrayList<>();
}
