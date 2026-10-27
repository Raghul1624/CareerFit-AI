package com.careerfit.service;

import com.careerfit.dto.InterviewDtos.*;
import com.careerfit.entity.*;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.CodeSubmissionRepository;
import com.careerfit.repository.InterviewCodingProblemRepository;
import com.careerfit.repository.InterviewTestCaseRepository;
import com.careerfit.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Run and submit code for the Coding Round. The code is executed ONLY by the external sandbox behind {@link CodeRunner};
 * if it is not configured or not reachable the user gets a clear error and no result is invented.
 *
 * <p>Each call is split into prepare (short transaction), execute in the sandbox (NO transaction, it can take seconds),
 * and persist (short transaction), so a slow sandbox never holds a database connection.
 */
@Service
@RequiredArgsConstructor
public class InterviewCodingService {

    private final InterviewAttemptService attemptService;
    private final CodeRunner runner;
    private final InterviewTestCaseRepository testCases;
    private final InterviewCodingProblemRepository problems;
    private final CodeSubmissionRepository submissions;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;

    @Value("${app.coding.max-submissions-per-problem:40}")
    private int maxSubmissions;

    /** Data copied out of the first transaction so the sandbox call needs no open transaction. */
    private record Prep(boolean expired, Long itemId, double timeLimitSeconds, int memoryLimitMb, List<InterviewTestCase> tests) {}

    public CodingStatus status() {
        boolean configured = runner.isConfigured();
        return new CodingStatus(configured, List.of(CodeLanguage.values()), configured ? "Code runs in OnlineCompiler's external sandbox."
            : "Code execution is not configured on this server, so Run and Submit are disabled. Set ONLINECOMPILER_API_KEY in the backend environment (see the README).");
    }

    // ------------------------------------------------------------------ run

    public RunResultView run(Long attemptId, Long questionId, RunRequest req) {
        ensureConfigured();
        boolean custom = req.stdin() != null && !req.stdin().isEmpty();
        Prep prep = tx.execute(s -> prepare(attemptId, questionId, req.sourceCode(), req.language(), false, !custom));
        failIfExpired(prep);
        CodeJudge judge = new CodeJudge(runner);
        try {
            if (custom) {
                CodeJudge.CustomResult r = judge.runCustom(req.language(), req.sourceCode(), req.stdin(), prep.timeLimitSeconds(), prep.memoryLimitMb());
                return new RunResultView(true, r.status(), r.stdout(), r.stderr(), r.compileOutput(), r.timeMs(), List.of(), null, null);
            }
            List<InterviewTestCase> samples = prep.tests().stream().filter(t -> !t.isHidden()).toList();
            CodeJudge.Report r = judge.judge(req.language(), req.sourceCode(), samples, prep.timeLimitSeconds(), prep.memoryLimitMb());
            return new RunResultView(false, r.verdict().name(), "", "", r.compileOutput() == null ? "" : r.compileOutput(), r.maxTimeMs(),
                r.cases().stream().map(InterviewCodingService::toView).toList(), r.passed(), r.total());
        } catch (CodeRunner.CodeExecutionException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ submit

    public SubmissionView submit(Long attemptId, Long questionId, SubmitCodeRequest req) {
        ensureConfigured();
        Prep prep = tx.execute(s -> prepare(attemptId, questionId, req.sourceCode(), req.language(), true, true));
        failIfExpired(prep);
        CodeJudge.Report report;
        try {
            report = new CodeJudge(runner).judge(req.language(), req.sourceCode(), prep.tests(), prep.timeLimitSeconds(), prep.memoryLimitMb());
        } catch (CodeRunner.CodeExecutionException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
        return tx.execute(s -> persist(attemptId, questionId, req, report));
    }

    public List<SubmissionView> list(Long attemptId, Long questionId) {
        return tx.execute(s -> {
            InterviewAttempt a = attemptService.owned(attemptId);
            InterviewAttemptItem item = codingItem(a, questionId);
            return submissions.findByAttemptItemIdOrderByCreatedAtDescIdDesc(item.getId()).stream().map(this::toView).toList();
        });
    }

    // ------------------------------------------------------------------ steps

    private Prep prepare(Long attemptId, Long questionId, String code, CodeLanguage language, boolean countSubmission, boolean saveDraft) {
        InterviewAttempt a = attemptService.owned(attemptId);
        if (a.isFinished()) throw new ApiException(HttpStatus.CONFLICT, "This attempt has already been submitted");
        if (attemptService.isPastDeadline(a)) {
            attemptService.finalizeAttempt(a, true);          // committed by the surrounding transaction; the 409 is raised afterwards
            return new Prep(true, null, 0, 0, List.of());
        }
        InterviewAttemptItem item = codingItem(a, questionId);
        InterviewCodingProblem problem = problems.findById(questionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coding problem not found"));
        if (countSubmission && submissions.countByAttemptItemId(item.getId()) >= maxSubmissions) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "You reached the limit of " + maxSubmissions + " submissions for this problem");
        }
        if (saveDraft) {                                      // keep the candidate's latest code so a reload does not lose it
            item.setAnswerText(code);
            item.setLanguage(language.name());
        }
        return new Prep(false, item.getId(), problem.getTimeLimitSeconds(), problem.getMemoryLimitMb(),
            testCases.findByQuestionIdOrderByDisplayOrderAsc(questionId));
    }

    private SubmissionView persist(Long attemptId, Long questionId, SubmitCodeRequest req, CodeJudge.Report report) {
        InterviewAttempt a = attemptService.owned(attemptId);
        InterviewAttemptItem item = codingItem(a, questionId);
        CodeSubmission s = new CodeSubmission();
        s.setAttemptItem(item);
        s.setUserId(currentUser.get().getId());
        s.setLanguage(req.language());
        s.setSourceCode(req.sourceCode());
        s.setStatus(report.verdict());
        s.setPassedTests(report.passed());
        s.setTotalTests(report.total());
        s.setScorePercent(report.scorePercent());
        s.setErrorOutput(report.compileOutput());
        s.setExecutionTimeMs(report.maxTimeMs());
        s.setResultsJson(toJson(report.cases().stream().map(InterviewCodingService::toView).toList()));
        submissions.save(s);
        item.setAnswerText(req.sourceCode());
        item.setLanguage(req.language().name());
        return toView(s);
    }

    // ------------------------------------------------------------------ helpers

    private void ensureConfigured() {
        if (!runner.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Code execution is not configured on this server. Set ONLINECOMPILER_API_KEY in the backend environment (see the README).");
        }
    }

    private static void failIfExpired(Prep prep) {
        if (prep.expired()) throw new ApiException(HttpStatus.CONFLICT, "Time is up: your attempt was submitted automatically");
    }

    private InterviewAttemptItem codingItem(InterviewAttempt a, Long questionId) {
        if (!a.getRound().isCoding()) throw new ApiException(HttpStatus.BAD_REQUEST, "This attempt is not a coding round");
        return a.getItems().stream().filter(i -> i.getQuestion().getId().equals(questionId)).findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "That question is not part of this attempt"));
    }

    private static CaseResultView toView(CodeJudge.CaseResult c) {
        return new CaseResultView(c.index(), c.hidden(), c.verdict(), c.input(), c.expectedOutput(), c.actualOutput(), c.errorOutput(), c.timeMs());
    }

    private SubmissionView toView(CodeSubmission s) {
        List<CaseResultView> cases = List.of();
        if (s.getResultsJson() != null) {
            try {
                cases = objectMapper.readValue(s.getResultsJson(), new TypeReference<List<CaseResultView>>() {});
            } catch (JsonProcessingException e) {
                cases = List.of();
            }
        }
        return new SubmissionView(s.getId(), s.getLanguage(), s.getStatus(), s.getPassedTests(), s.getTotalTests(), s.getScorePercent(),
            s.getErrorOutput(), s.getExecutionTimeMs(), s.getCreatedAt(), cases, s.getSourceCode());
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise results", e);
        }
    }
}
