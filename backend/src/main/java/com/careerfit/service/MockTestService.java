package com.careerfit.service;

import com.careerfit.dto.TestDtos.MockTestSummary;
import com.careerfit.dto.TestDtos.SkillCoverage;
import com.careerfit.entity.CareerPath;
import com.careerfit.entity.Difficulty;
import com.careerfit.entity.MockTest;
import com.careerfit.entity.Skill;
import com.careerfit.entity.TestMode;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.MockTestRepository;
import com.careerfit.repository.PoolStat;
import com.careerfit.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Read side of the mock tests: lists tests and describes what each one covers.
 * A career test covers the skills of its career path; a skill test covers exactly one skill.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MockTestService {

    private final MockTestRepository tests;
    private final QuestionRepository questions;

    /** What the question bank holds for one skill. */
    private record SkillPool(int count, Set<String> topics, Set<Difficulty> difficulties) {}

    public List<MockTestSummary> list(Long careerPathId, TestMode mode, Long skillId) {
        Map<Long, SkillPool> pool = skillPools();
        return tests.findAllActiveWithSkills().stream()
            .filter(t -> mode == null || t.getMode() == mode)
            .filter(t -> careerPathId == null
                || (t.getCareerPath() != null && t.getCareerPath().getId().equals(careerPathId)))
            .filter(t -> skillId == null || t.eligibleSkillIds().contains(skillId))
            .map(t -> toSummary(t, pool))
            .toList();
    }

    public MockTestSummary get(Long id) {
        MockTest t = tests.findById(id).filter(MockTest::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test not found"));
        return toSummary(t, skillPools());
    }

    /**
     * Tests for a career path page: the path's own career mock test first, then the individual
     * skill tests for the skills the path requires. Career tests of OTHER paths are not listed.
     */
    public List<MockTestSummary> forCareerPath(CareerPath path) {
        Set<Long> pathSkillIds = path.getSkills().stream().map(Skill::getId).collect(Collectors.toSet());
        Map<Long, SkillPool> pool = skillPools();
        return tests.findAllActiveWithSkills().stream()
            .filter(t -> isCareerTestOf(t, path.getId())
                || (t.getMode() == TestMode.SKILL && pathSkillIds.containsAll(t.eligibleSkillIds())))
            .sorted(Comparator.comparing((MockTest t) -> t.getMode() != TestMode.CAREER)
                .thenComparing(MockTest::getTitle))
            .map(t -> toSummary(t, pool))
            .toList();
    }

    /** The one career mock test of a path, if it has one. */
    public Optional<MockTestSummary> careerTestFor(CareerPath path) {
        return forCareerPath(path).stream().filter(s -> s.mode() == TestMode.CAREER).findFirst();
    }

    /** Skills with the topics their questions cover: "recommended learning topics" for a career path. */
    public List<SkillCoverage> coverageOf(Collection<Skill> skills) {
        Map<Long, SkillPool> pool = skillPools();
        return skills.stream()
            .sorted(Comparator.comparing(Skill::getName))
            .map(s -> coverage(s, pool))
            .filter(c -> c.questionCount() > 0)
            .toList();
    }

    private static boolean isCareerTestOf(MockTest t, Long pathId) {
        return t.getMode() == TestMode.CAREER && t.getCareerPath() != null && t.getCareerPath().getId().equals(pathId);
    }

    private Map<Long, SkillPool> skillPools() {
        Map<Long, Integer> counts = new HashMap<>();
        Map<Long, Set<String>> topics = new HashMap<>();
        Map<Long, Set<Difficulty>> difficulties = new HashMap<>();
        for (PoolStat s : questions.poolStats()) {
            counts.merge(s.skillId(), s.count().intValue(), Integer::sum);
            if (s.topic() != null && !s.topic().isBlank()) {
                topics.computeIfAbsent(s.skillId(), k -> new TreeSet<>()).add(s.topic());
            }
            difficulties.computeIfAbsent(s.skillId(), k -> EnumSet.noneOf(Difficulty.class)).add(s.difficulty());
        }
        Map<Long, SkillPool> result = new HashMap<>();
        counts.forEach((id, count) -> result.put(id, new SkillPool(count,
            topics.getOrDefault(id, Set.of()), difficulties.getOrDefault(id, Set.of()))));
        return result;
    }

    private SkillCoverage coverage(Skill skill, Map<Long, SkillPool> pool) {
        SkillPool p = pool.get(skill.getId());
        return p == null
            ? new SkillCoverage(skill.getId(), skill.getName(), 0, List.of())
            : new SkillCoverage(skill.getId(), skill.getName(), p.count(), List.copyOf(p.topics()));
    }

    private MockTestSummary toSummary(MockTest t, Map<Long, SkillPool> pool) {
        Set<Long> eligible = t.eligibleSkillIds();
        List<SkillCoverage> coverage = t.getSkills().stream()
            .filter(s -> eligible.contains(s.getId()))
            .sorted(Comparator.comparing(Skill::getName))
            .map(s -> coverage(s, pool))
            .toList();
        int available = coverage.stream().mapToInt(SkillCoverage::questionCount).sum();
        Set<Difficulty> levels = EnumSet.noneOf(Difficulty.class);
        for (Long id : eligible) {
            SkillPool p = pool.get(id);
            if (p != null) levels.addAll(p.difficulties());
        }
        List<String> topics = coverage.stream().flatMap(c -> c.topics().stream()).distinct().toList();
        boolean startable = t.getQuestionCount() > 0 && available >= t.getQuestionCount();
        String reason = startable ? null : eligible.isEmpty()
            ? "This test has no skills configured yet."
            : "This test needs " + t.getQuestionCount() + " questions but only " + available + " are available yet.";
        return new MockTestSummary(t.getId(), t.getTitle(), t.getDescription(), t.getMode(),
            t.getCareerPath() == null ? null : t.getCareerPath().getId(),
            t.getCareerPath() == null ? null : t.getCareerPath().getName(),
            coverage.stream().map(SkillCoverage::skillName).toList(),
            t.getDurationMinutes(), t.getQuestionCount(), t.getMarksPerQuestion(),
            t.getQuestionCount() * t.getMarksPerQuestion(), t.getNegativeMarks(),
            t.getPassingPercentage(), t.getMaxAttempts(),
            coverage.stream().map(SkillCoverage::skillId).toList(), coverage, topics,
            List.copyOf(levels), available, startable, reason);
    }
}
