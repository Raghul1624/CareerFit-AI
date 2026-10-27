package com.careerfit.controller;

import com.careerfit.dto.TestDtos.AttemptView;
import com.careerfit.dto.TestDtos.MockTestSummary;
import com.careerfit.entity.TestMode;
import com.careerfit.service.AttemptService;
import com.careerfit.service.MockTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mock-tests")
@RequiredArgsConstructor
public class MockTestController {

    private final MockTestService service;
    private final AttemptService attemptService;

    @GetMapping
    public List<MockTestSummary> list(@RequestParam(required = false) Long careerPathId,
                                      @RequestParam(required = false) TestMode mode,
                                      @RequestParam(required = false) Long skillId) {
        return service.list(careerPathId, mode, skillId);
    }

    @GetMapping("/{id}")
    public MockTestSummary get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/attempts")
    public ResponseEntity<AttemptView> start(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attemptService.start(id));
    }
}
