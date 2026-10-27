package com.careerfit.controller;

import com.careerfit.dto.CommonDtos.PageResponse;
import com.careerfit.dto.InterviewDtos.*;
import com.careerfit.entity.InterviewRound;
import com.careerfit.service.InterviewAttemptService;
import com.careerfit.service.InterviewCatalogService;
import com.careerfit.service.InterviewCodingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Interview Preparation: roles and rounds, assessments, coding, results and history. Every endpoint requires authentication. */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewCatalogService catalog;
    private final InterviewAttemptService attempts;
    private final InterviewCodingService coding;

    @GetMapping("/roles")
    public List<RoleInfo> roles() {
        return catalog.roles();
    }

    @GetMapping("/roles/{code}")
    public RoleInfo role(@PathVariable String code) {
        return catalog.role(code);
    }

    @GetMapping("/overview")
    public InterviewOverview overview() {
        return catalog.overview();
    }

    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptView start(@Valid @RequestBody StartRequest request) {
        return attempts.start(request);
    }

    // NOTE: the literal /history path is matched before /{id}
    @GetMapping("/attempts/history")
    public PageResponse<HistoryItem> history(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) InterviewRound round,
                                             @RequestParam(required = false) String role) {
        return attempts.history(page, size, round, role);
    }

    @GetMapping("/attempts/{id}")
    public AttemptView get(@PathVariable Long id) {
        return attempts.get(id);
    }

    @PutMapping("/attempts/{id}/answers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveAnswers(@PathVariable Long id, @Valid @RequestBody SaveAnswersRequest request) {
        attempts.saveAnswers(id, request);
    }

    @PostMapping("/attempts/{id}/submit")
    public ResultView submit(@PathVariable Long id) {
        return attempts.submit(id);
    }

    @GetMapping("/attempts/{id}/result")
    public ResultView result(@PathVariable Long id) {
        return attempts.result(id);
    }

    // ---- coding round
    @GetMapping("/coding/status")
    public CodingStatus codingStatus() {
        return coding.status();
    }

    @PostMapping("/attempts/{id}/questions/{questionId}/run")
    public RunResultView run(@PathVariable Long id, @PathVariable Long questionId, @Valid @RequestBody RunRequest request) {
        return coding.run(id, questionId, request);
    }

    @PostMapping("/attempts/{id}/questions/{questionId}/submit-code")
    public SubmissionView submitCode(@PathVariable Long id, @PathVariable Long questionId, @Valid @RequestBody SubmitCodeRequest request) {
        return coding.submit(id, questionId, request);
    }

    @GetMapping("/attempts/{id}/questions/{questionId}/submissions")
    public List<SubmissionView> submissions(@PathVariable Long id, @PathVariable Long questionId) {
        return coding.list(id, questionId);
    }
}
