package com.careerfit.service;

import com.careerfit.entity.CodeLanguage;
import com.careerfit.entity.CodeVerdict;
import com.careerfit.entity.InterviewTestCase;
import com.careerfit.service.CodeRunner.CodeExecutionException;
import com.careerfit.service.CodeRunner.RunOutcome;
import com.careerfit.service.CodeRunner.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a submission against test cases through a {@link CodeRunner} and decides the verdicts. It never executes code
 * itself and never invents a result: if the runner fails, a {@link CodeExecutionException} propagates.
 * Hidden test cases are judged but nothing about them (input, expected or actual output, stderr) is returned.
 */
public class CodeJudge {

    public record CaseResult(int index, boolean hidden, String verdict, String input, String expectedOutput,
                             String actualOutput, String errorOutput, Integer timeMs) {}

    public record Report(CodeVerdict verdict, int passed, int total, double scorePercent, List<CaseResult> cases,
                         String compileOutput, Integer maxTimeMs) {}

    public record CustomResult(String status, String stdout, String stderr, String compileOutput, Integer timeMs) {}

    public static final String PASSED = "PASSED";
    public static final String WRONG_ANSWER = "WRONG_ANSWER";
    public static final String RUNTIME_ERROR = "RUNTIME_ERROR";
    public static final String TIME_LIMIT_EXCEEDED = "TIME_LIMIT_EXCEEDED";
    public static final String COMPILATION_ERROR = "COMPILATION_ERROR";

    private static final int MAX_OUTPUT_CHARS = 4000;

    private final CodeRunner runner;

    public CodeJudge(CodeRunner runner) {
        this.runner = runner;
    }

    /** Runs the given test cases in order. A compilation error stops the run, because every case would fail the same way. */
    public Report judge(CodeLanguage language, String code, List<InterviewTestCase> tests, double timeLimitSeconds, int memoryLimitMb) {
        List<CaseResult> results = new ArrayList<>();
        int passed = 0;
        Integer maxTime = null;
        String compileOutput = null;
        boolean anyTle = false, anyRuntime = false;
        int index = 0;
        for (InterviewTestCase tc : tests) {
            index++;
            RunOutcome out = runner.execute(language, code, tc.getInput(), timeLimitSeconds, memoryLimitMb);
            if (out.status() == Status.INTERNAL_ERROR) {
                throw new CodeExecutionException("The code execution service reported an internal error"
                    + (out.message().isBlank() ? "" : ": " + out.message()));
            }
            if (out.timeMs() != null) maxTime = maxTime == null ? out.timeMs() : Math.max(maxTime, out.timeMs());
            String verdict;
            switch (out.status()) {
                case COMPILATION_ERROR -> {
                    verdict = COMPILATION_ERROR;
                    compileOutput = clip(out.compileOutput().isBlank() ? out.message() : out.compileOutput());
                }
                case TIME_LIMIT_EXCEEDED -> {
                    verdict = TIME_LIMIT_EXCEEDED;
                    anyTle = true;
                }
                case RUNTIME_ERROR -> {
                    verdict = RUNTIME_ERROR;
                    anyRuntime = true;
                }
                default -> verdict = OutputComparator.matches(tc.getExpectedOutput(), out.stdout()) ? PASSED : WRONG_ANSWER;
            }
            if (PASSED.equals(verdict)) passed++;
            results.add(tc.isHidden()
                ? new CaseResult(index, true, verdict, null, null, null, null, out.timeMs())
                : new CaseResult(index, false, verdict, tc.getInput(), tc.getExpectedOutput(), clip(out.stdout()),
                    clip(out.stderr().isBlank() ? out.message() : out.stderr()), out.timeMs()));
            if (COMPILATION_ERROR.equals(verdict)) break;
        }
        int total = tests.size();
        CodeVerdict overall;
        if (compileOutput != null) overall = CodeVerdict.COMPILATION_ERROR;
        else if (passed == total && total > 0) overall = CodeVerdict.ACCEPTED;
        else if (passed > 0) overall = CodeVerdict.PARTIAL;
        else if (anyTle) overall = CodeVerdict.TIME_LIMIT_EXCEEDED;
        else if (anyRuntime) overall = CodeVerdict.RUNTIME_ERROR;
        else overall = CodeVerdict.WRONG_ANSWER;
        double score = total == 0 ? 0 : Math.round(passed * 10000.0 / total) / 100.0;
        return new Report(overall, passed, total, score, results, compileOutput, maxTime);
    }

    /** "Run" with custom input: only the program output, no verdict (there is no expected output to compare). */
    public CustomResult runCustom(CodeLanguage language, String code, String stdin, double timeLimitSeconds, int memoryLimitMb) {
        RunOutcome out = runner.execute(language, code, stdin, timeLimitSeconds, memoryLimitMb);
        if (out.status() == Status.INTERNAL_ERROR) {
            throw new CodeExecutionException("The code execution service reported an internal error"
                + (out.message().isBlank() ? "" : ": " + out.message()));
        }
        return new CustomResult(out.status().name(), clip(out.stdout()), clip(out.stderr().isBlank() ? out.message() : out.stderr()),
            clip(out.compileOutput()), out.timeMs());
    }

    private static String clip(String s) {
        if (s == null) return "";
        return s.length() <= MAX_OUTPUT_CHARS ? s : s.substring(0, MAX_OUTPUT_CHARS) + "\n... (output truncated)";
    }
}
