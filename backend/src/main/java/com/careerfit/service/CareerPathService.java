package com.careerfit.service;

import com.careerfit.dto.CareerDtos.*;
import com.careerfit.dto.TestDtos;
import com.careerfit.entity.CareerPath;
import com.careerfit.entity.TestMode;
import com.careerfit.exception.ApiException;
import com.careerfit.entity.Skill;
import com.careerfit.repository.CareerPathRepository;
import com.careerfit.repository.LearningRoadmapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerPathService {

    private final CareerPathRepository paths;
    private final MockTestService mockTestService;
    private final LearningRoadmapRepository roadmaps;

    public List<CareerPathSummary> list() {
        return paths.findAllActiveWithCategory().stream()
            .map(p -> new CareerPathSummary(p.getId(), p.getName(), p.getDescription(),
                p.getCategory().getId(), p.getCategory().getName()))
            .toList();
    }

    public CareerPathDetail get(Long id) {
        CareerPath p = paths.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Career path not found"));
        List<SkillDto> skills = p.getSkills().stream()
            .sorted(Comparator.comparing(s -> s.getName()))
            .map(s -> new SkillDto(s.getId(), s.getName(), s.getDescription()))
            .toList();
        List<TestDtos.MockTestSummary> recommended = mockTestService.forCareerPath(p);
        TestDtos.MockTestSummary careerTest = recommended.stream()
            .filter(t -> t.mode() == TestMode.CAREER).findFirst().orElse(null);
        RoadmapRef roadmap = roadmaps.findByCareerPathId(p.getId())
            .map(r -> new RoadmapRef(r.getId(), r.getTitle(), r.getItems().size()))
            .orElse(null);
        // Foundations first: skills are seeded from basic to advanced, so id order is a sensible default.
        List<String> order = p.getSkills().stream()
            .sorted(Comparator.comparing(Skill::getId)).map(Skill::getName).toList();
        return new CareerPathDetail(p.getId(), p.getName(), p.getDescription(), p.getCategory().getName(),
            skills, recommended, careerTest, mockTestService.coverageOf(p.getSkills()), roadmap, order);
    }
}
