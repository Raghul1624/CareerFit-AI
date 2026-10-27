package com.careerfit.controller;

import com.careerfit.dto.Phase5Dtos.*;
import com.careerfit.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LearningController {

    private final LearningService service;

    @GetMapping("/api/learning-roadmaps")
    public List<RoadmapSummary> list() {
        return service.list();
    }

    @GetMapping("/api/learning-roadmaps/{id}")
    public RoadmapDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    /** The id is a roadmap ITEM (step) id. The user always comes from the security context. */
    @PutMapping("/api/learning-progress/{itemId}")
    public ProgressResult progress(@PathVariable Long itemId, @Valid @RequestBody ProgressRequest request) {
        return service.setProgress(itemId, request.completed());
    }
}
