package com.careerfit.service;

import com.careerfit.dto.DashboardDtos.*;
import com.careerfit.dto.TestDtos.AttemptSummary;
import com.careerfit.entity.AttemptStatus;
import com.careerfit.entity.CareerPath;
import com.careerfit.entity.Skill;
import com.careerfit.entity.User;
import com.careerfit.entity.UserProfile;
import com.careerfit.repository.TestAttemptRepository;
import com.careerfit.repository.UserProfileRepository;
import com.careerfit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final double WEAK_THRESHOLD = 60.0;

    private final CurrentUser currentUser;
    private final TestAttemptRepository attempts;
    private final UserProfileRepository profiles;
    private final AttemptService attemptService;

    public Summary summary() {
        User user = currentUser.get();
        AttemptStatus excluded = AttemptStatus.IN_PROGRESS;
        long completed = attempts.countByUserIdAndStatusNot(user.getId(), excluded);

        List<AttemptSummary> recent = attempts
            .findTop5ByUserIdAndStatusNotOrderByStartedAtDesc(user.getId(), excluded)
            .stream().map(attemptService::summaryOf).toList();

        List<SkillPerformance> skills = new ArrayList<>();
        Set<String> attemptedSkills = new HashSet<>();
        for (Object[] row : attempts.skillPerformance(user.getId(), excluded)) {
            String name = (String) row[0];
            long correct = row[1] == null ? 0 : ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            double accuracy = total == 0 ? 0 : Math.round(correct * 1000.0 / total) / 10.0;
            skills.add(new SkillPerformance(name, correct, total, accuracy));
            attemptedSkills.add(name);
        }

        List<String> recommendations = new ArrayList<>();
        skills.stream().filter(s -> s.accuracy() < WEAK_THRESHOLD).forEach(s ->
            recommendations.add("Revise " + s.skill() + " (accuracy " + s.accuracy() + "%)"));

        UserProfile profile = profiles.findByUserId(user.getId()).orElse(null);
        String targetName = null;
        if (profile != null && profile.getTargetCareerPath() != null) {
            CareerPath path = profile.getTargetCareerPath();
            targetName = path.getName();
            path.getSkills().stream().map(Skill::getName)
                .filter(n -> !attemptedSkills.contains(n)).sorted().limit(3)
                .forEach(n -> recommendations.add("Start practising " + n + " (required for " + path.getName() + ")"));
        }

        Double avg = completed == 0 ? null : round1(attempts.averagePercentage(user.getId(), excluded));
        Double best = completed == 0 ? null : round1(attempts.bestPercentage(user.getId(), excluded));
        return new Summary(user.getFullName(), targetName, completed, avg, best, recent, skills, recommendations);
    }

    private static Double round1(Double v) {
        return v == null ? null : Math.round(v * 10.0) / 10.0;
    }
}
