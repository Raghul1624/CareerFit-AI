package com.careerfit.service;

import com.careerfit.dto.InterviewDtos.*;
import com.careerfit.entity.*;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.InterviewAttemptRepository;
import com.careerfit.repository.InterviewQuestionRepository;
import com.careerfit.repository.InterviewRoleRepository;
import com.careerfit.repository.InterviewRoundConfigRepository;
import com.careerfit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Roles, rounds and difficulty levels, with availability. A level is only offered when the number of servable questions
 * for that role, round and level reaches the configured test size.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewCatalogService {

    private final InterviewRoleRepository roles;
    private final InterviewRoundConfigRepository configs;
    private final InterviewQuestionRepository questions;
    private final InterviewAttemptRepository attempts;
    private final CurrentUser currentUser;
    private final InterviewAttemptService attemptService;

    private record Key(Long roleId, InterviewRound round, InterviewLevel level) {}

    public List<RoleInfo> roles() {
        Map<Key, Integer> counts = servableCounts();
        List<InterviewRoundConfig> allConfigs = configs.findAll();
        List<RoleInfo> result = new ArrayList<>();
        for (InterviewRole role : roles.findByActiveTrueOrderByDisplayOrderAsc()) {
            result.add(toInfo(role, allConfigs, counts));
        }
        return result;
    }

    public RoleInfo role(String code) {
        InterviewRole role = roles.findByCodeAndActiveTrue(code.toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview role not found"));
        return toInfo(role, configs.findAll(), servableCounts());
    }

    public InterviewOverview overview() {
        Long userId = currentUser.get().getId();
        List<HistoryItem> recent = attempts.findTop5ByUserIdOrderByStartedAtDesc(userId).stream()
            .map(attemptService::toHistoryItem).toList();
        return new InterviewOverview(attempts.countByUserId(userId), recent);
    }

    private Map<Key, Integer> servableCounts() {
        Map<Key, Integer> counts = new HashMap<>();
        for (Object[] row : questions.countServable()) {
            counts.put(new Key((Long) row[0], (InterviewRound) row[1], (InterviewLevel) row[2]), ((Number) row[3]).intValue());
        }
        return counts;
    }

    private RoleInfo toInfo(InterviewRole role, List<InterviewRoundConfig> allConfigs, Map<Key, Integer> counts) {
        List<RoundInfo> rounds = new ArrayList<>();
        for (InterviewRound round : InterviewRound.values()) {
            List<LevelAvailability> levels = new ArrayList<>();
            for (InterviewLevel level : InterviewLevel.values()) {
                Optional<InterviewRoundConfig> cfg = allConfigs.stream()
                    .filter(c -> c.getRound() == round && c.getLevel() == level).findFirst();
                int available = counts.getOrDefault(new Key(role.getId(), round, level), 0);
                if (cfg.isEmpty()) {
                    levels.add(new LevelAvailability(level, false, 0, 0, 0, available, "This level is not configured yet."));
                    continue;
                }
                InterviewRoundConfig c = cfg.get();
                boolean ok = c.getQuestionCount() > 0 && available >= c.getQuestionCount();
                levels.add(new LevelAvailability(level, ok, c.getQuestionCount(), c.getDurationMinutes(), c.getPassingPercentage(),
                    available, ok ? null : "Not available yet: needs " + c.getQuestionCount() + " questions but only " + available + " exist."));
            }
            rounds.add(new RoundInfo(round, round.label(), round.description(), levels));
        }
        return new RoleInfo(role.getId(), role.getCode(), role.getName(), role.getDescription(), rounds);
    }
}
