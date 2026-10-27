package com.careerfit.controller;

import com.careerfit.dto.Phase5Dtos.PerformanceView;
import com.careerfit.dto.Phase5Dtos.SkillGap;
import com.careerfit.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService service;

    @GetMapping("/performance")
    public PerformanceView performance() {
        return service.performance();
    }

    @GetMapping("/skill-gap")
    public SkillGap skillGap() {
        return service.skillGap();
    }
}
