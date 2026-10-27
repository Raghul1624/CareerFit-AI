package com.careerfit.service;

import com.careerfit.dto.DashboardDtos.SkillPerformance;
import com.careerfit.dto.Phase5Dtos.*;
import com.careerfit.entity.*;
import com.careerfit.repository.*;
import com.careerfit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based, explainable analytics.
 * Skill status rules (per skill required by the target career):
 *   STRONG     - answered test questions with accuracy >= 70%
 *   IMPROVE    - answered test questions with accuracy < 70%
 *   UNVERIFIED - no test data yet, but the user declared the skill
 *   MISSING    - no test data and not declared
 * Unanswered questions count as not correct.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    static final double STRONG_THRESHOLD = 70.0;

    private final CurrentUser currentUser;
    private final TestAttemptRepository attempts;
    private final UserProfileRepository profiles;
    private final LearningRoadmapRepository roadmaps;
    private final MockTestRepository tests;

    public PerformanceView performance() {
        User user = currentUser.get();
        List<TestAttempt> list = new ArrayList<>(
            attempts.findTop50ByUserIdAndStatusNotOrderByStartedAtDesc(user.getId(), AttemptStatus.IN_PROGRESS));
        Collections.reverse(list);
        List<TrendPoint> trend = list.stream()
            .map(a -> new TrendPoint(a.getId(), a.getMockTest().getTitle(), a.getStartedAt(),
                a.getPercentage() == null ? 0 : a.getPercentage()))
            .toList();
        List<SkillPerformance> skills = new ArrayList<>();
        for (Object[] row : attempts.skillPerformance(user.getId(), AttemptStatus.IN_PROGRESS)) {
            long correct = row[1] == null ? 0 : ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            skills.add(new SkillPerformance((String) row[0], correct, total,
                total == 0 ? 0 : Math.round(correct * 1000.0 / total) / 10.0));
        }
        return new PerformanceView(trend, skills);
    }

    public SkillGap skillGap() {
        User user = currentUser.get();
        UserProfile profile = profiles.findByUserId(user.getId()).orElse(null);
        if (profile == null || profile.getTargetCareerPath() == null) {
            return new SkillGap(null, null, List.of(), List.of());
        }
        CareerPath path = profile.getTargetCareerPath();

        Map<String, long[]> perf = new HashMap<>();
        for (Object[] row : attempts.skillPerformance(user.getId(), AttemptStatus.IN_PROGRESS)) {
            perf.put((String) row[0], new long[]{row[1] == null ? 0 : ((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        Set<Long> declared = user.getDeclaredSkills().stream().map(Skill::getId).collect(Collectors.toSet());
        List<MockTest> activeTests = tests.findByActiveTrueOrderByTitleAsc();

        List<GapItem> items = new ArrayList<>();
        for (Skill skill : path.getSkills().stream().sorted(Comparator.comparing(Skill::getName)).toList()) {
            long[] p = perf.get(skill.getName());
            Double accuracy = (p == null || p[1] == 0) ? null : Math.round(p[0] * 1000.0 / p[1]) / 10.0;
            boolean isDeclared = declared.contains(skill.getId());
            String status;
            String reason;
            if (accuracy != null) {
                status = accuracy >= STRONG_THRESHOLD ? "STRONG" : "IMPROVE";
                reason = accuracy + "% correct across " + p[1] + " test question(s)";
            } else if (isDeclared) {
                status = "UNVERIFIED";
                reason = "You listed this skill, but no test has verified it yet";
            } else {
                status = "MISSING";
                reason = "Required for " + path.getName() + "; not declared and not yet tested";
            }
            List<TestRef> refs = "STRONG".equals(status) ? List.of() : activeTests.stream()
                .filter(t -> t.getSkills().stream().anyMatch(s -> s.getId().equals(skill.getId())))
                .map(t -> new TestRef(t.getId(), t.getTitle())).toList();
            items.add(new GapItem(skill.getId(), skill.getName(), status, accuracy, isDeclared, reason, refs));
        }

        // Suggested order follows the career roadmap; skills not in the roadmap go last.
        Map<String, Integer> firstStep = new HashMap<>();
        roadmaps.findByCareerPathId(path.getId()).ifPresent(r -> r.getItems().forEach(i ->
            i.getSkills().forEach(s -> firstStep.merge(s.getName(), i.getStepOrder(), Math::min))));
        List<String> order = items.stream().filter(g -> !"STRONG".equals(g.status()))
            .sorted(Comparator.<GapItem>comparingInt(g -> firstStep.getOrDefault(g.skill(), Integer.MAX_VALUE))
                .thenComparing(GapItem::skill))
            .map(GapItem::skill).toList();
        return new SkillGap(path.getId(), path.getName(), items, order);
    }
}
