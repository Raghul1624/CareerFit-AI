package com.careerfit.service;

import com.careerfit.dto.Phase5Dtos.*;
import com.careerfit.entity.*;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.*;
import com.careerfit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningService {

    private final CurrentUser currentUser;
    private final LearningRoadmapRepository roadmaps;
    private final LearningItemRepository itemsRepo;
    private final UserLearningProgressRepository progress;
    private final MockTestRepository tests;

    @Transactional(readOnly = true)
    public List<RoadmapSummary> list() {
        Map<Long, UserLearningProgress> done = completedByItem(currentUser.get().getId());
        return roadmaps.findAllWithCareerPath().stream().map(r -> {
            int total = r.getItems().size();
            int completed = (int) r.getItems().stream().filter(i -> done.containsKey(i.getId())).count();
            return new RoadmapSummary(r.getId(), r.getCareerPath().getId(), r.getCareerPath().getName(),
                r.getTitle(), r.getDescription(), total, completed);
        }).toList();
    }

    @Transactional(readOnly = true)
    public RoadmapDetail get(Long id) {
        Map<Long, UserLearningProgress> done = completedByItem(currentUser.get().getId());
        LearningRoadmap r = roadmaps.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Roadmap not found"));
        List<MockTest> activeTests = tests.findByActiveTrueOrderByTitleAsc();
        List<RoadmapItemDto> items = r.getItems().stream().map(i -> {
            Set<Long> skillIds = i.getSkills().stream().map(Skill::getId).collect(Collectors.toSet());
            List<TestRef> refs = activeTests.stream()
                .filter(t -> t.getSkills().stream().anyMatch(s -> skillIds.contains(s.getId())))
                .map(t -> new TestRef(t.getId(), t.getTitle())).toList();
            UserLearningProgress p = done.get(i.getId());
            return new RoadmapItemDto(i.getId(), i.getStepOrder(), i.getTitle(), i.getDescription(),
                i.getEstimatedDuration(), i.getSkills().stream().map(Skill::getName).sorted().toList(),
                p != null, p == null ? null : p.getCompletedAt(), refs);
        }).toList();
        int completed = (int) items.stream().filter(RoadmapItemDto::completed).count();
        double pct = items.isEmpty() ? 0 : Math.round(completed * 1000.0 / items.size()) / 10.0;
        return new RoadmapDetail(r.getId(), r.getCareerPath().getId(), r.getCareerPath().getName(),
            r.getTitle(), r.getDescription(), items.size(), completed, pct, items);
    }

    @Transactional
    public ProgressResult setProgress(Long itemId, boolean completed) {
        User user = currentUser.get();
        LearningRoadmapItem item = itemsRepo.findById(itemId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Roadmap step not found"));
        UserLearningProgress p = progress.findByUserIdAndItemId(user.getId(), itemId).orElseGet(() -> {
            UserLearningProgress np = new UserLearningProgress();
            np.setUser(user);
            np.setItem(item);
            return np;
        });
        p.setCompleted(completed);
        p.setCompletedAt(completed ? Instant.now() : null);
        progress.save(p);
        return new ProgressResult(itemId, completed, p.getCompletedAt());
    }

    private Map<Long, UserLearningProgress> completedByItem(Long userId) {
        return progress.findByUserIdAndCompletedTrue(userId).stream()
            .collect(Collectors.toMap(p -> p.getItem().getId(), p -> p));
    }
}
