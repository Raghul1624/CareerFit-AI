package com.careerfit.controller;

import com.careerfit.dto.CommonDtos.PageResponse;
import com.careerfit.dto.TestDtos.*;
import com.careerfit.service.AttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService service;

    // NOTE: /history must be declared before /{id} patterns; Spring prefers the literal match anyway.
    @GetMapping("/history")
    public PageResponse<AttemptSummary> history(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(defaultValue = "date") String sort,
                                                @RequestParam(defaultValue = "desc") String direction) {
        return service.history(page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public AttemptView get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}/answers")
    public ResponseEntity<Void> saveAnswers(@PathVariable Long id, @Valid @RequestBody SaveAnswersRequest request) {
        service.saveAnswers(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    public ResultView submit(@PathVariable Long id) {
        return service.submit(id);
    }

    @GetMapping("/{id}/result")
    public ResultView result(@PathVariable Long id) {
        return service.result(id);
    }
}
