package com.careerfit.service;

import com.careerfit.entity.CodeLanguage;
import com.careerfit.entity.CodeVerdict;
import com.careerfit.entity.InterviewTestCase;
import com.careerfit.service.CodeRunner.CodeExecutionException;
import com.careerfit.service.CodeRunner.RunOutcome;
import com.careerfit.service.CodeRunner.Status;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** Verdict logic, hidden-test masking and output comparison, using a scripted runner (no sandbox needed). */
class CodeJudgeTest {

    /** A fake sandbox that answers from a function of the stdin. It stands in for Judge0 only in tests. */
    private static CodeRunner runner(Function<String, RunOutcome> answer, List<String> calls) {
        return new CodeRunner() {
            public boolean isConfigured() { return true; }
            public RunOutcome execute(CodeLanguage l, String code, String stdin, double t, int m) {
                calls.add(stdin);
                return answer.apply(stdin);
            }
        };
    }

    private static InterviewTestCase tc(String in, String out, boolean hidden) {
        InterviewTestCase t = new InterviewTestCase();
        t.setInput(in);
        t.setExpectedOutput(out);
        t.setHidden(hidden);
        return t;
    }

    private static final List<InterviewTestCase> TESTS = List.of(tc("1\n", "2", false), tc("2\n", "4", false), tc("3\n", "6", true), tc("4\n", "8", true));

    private static RunOutcome ok(String out) {
        return RunOutcome.of(Status.OK, out, "", "", 12, "");
    }

    private static Function<String, RunOutcome> doubling() {
        return in -> ok(String.valueOf(Integer.parseInt(in.trim()) * 2) + "\n");
    }

    @Test
    void aCorrectProgramIsAcceptedWithFullScore() {
        CodeJudge.Report r = new CodeJudge(runner(doubling(), new ArrayList<>())).judge(CodeLanguage.PYTHON, "x", TESTS, 2, 256);
        assertEquals(CodeVerdict.ACCEPTED, r.verdict());
        assertEquals(4, r.passed());
        assertEquals(100.0, r.scorePercent(), 0.001);
    }

    @Test
    void aPartiallyCorrectProgramGetsProportionalScore() {
        Function<String, RunOutcome> buggy = in -> ok(in.trim().equals("4") ? "9\n" : String.valueOf(Integer.parseInt(in.trim()) * 2));
        CodeJudge.Report r = new CodeJudge(runner(buggy, new ArrayList<>())).judge(CodeLanguage.JAVA, "x", TESTS, 2, 256);
        assertEquals(CodeVerdict.PARTIAL, r.verdict());
        assertEquals(3, r.passed());
        assertEquals(75.0, r.scorePercent(), 0.001);
        assertEquals(CodeJudge.WRONG_ANSWER, r.cases().get(3).verdict());
    }

    @Test
    void aProgramThatPrintsTheWrongAnswerEverywhereIsWrongAnswerWithZero() {
        CodeJudge.Report r = new CodeJudge(runner(in -> ok("0"), new ArrayList<>())).judge(CodeLanguage.PYTHON, "x", TESTS, 2, 256);
        assertEquals(CodeVerdict.WRONG_ANSWER, r.verdict());
        assertEquals(0.0, r.scorePercent(), 0.001);
    }

    @Test
    void compilationErrorsStopTheRunAndExposeTheCompilerMessage() {
        List<String> calls = new ArrayList<>();
        CodeRunner compileFail = runner(in -> RunOutcome.of(Status.COMPILATION_ERROR, "", "", "Main.java:3: error: ';' expected", null, ""), calls);
        CodeJudge.Report r = new CodeJudge(compileFail).judge(CodeLanguage.JAVA, "x", TESTS, 2, 256);
        assertEquals(CodeVerdict.COMPILATION_ERROR, r.verdict());
        assertTrue(r.compileOutput().contains("';' expected"));
        assertEquals(1, calls.size(), "no point running the other cases after a compilation error");
        assertEquals(0, r.passed());
    }

    @Test
    void runtimeErrorsAndTimeoutsGetTheirOwnVerdicts() {
        CodeJudge.Report runtime = new CodeJudge(runner(in -> RunOutcome.of(Status.RUNTIME_ERROR, "", "ZeroDivisionError", "", 5, ""), new ArrayList<>()))
            .judge(CodeLanguage.PYTHON, "x", TESTS, 2, 256);
        assertEquals(CodeVerdict.RUNTIME_ERROR, runtime.verdict());
        assertTrue(runtime.cases().get(0).errorOutput().contains("ZeroDivisionError"));

        CodeJudge.Report tle = new CodeJudge(runner(in -> RunOutcome.of(Status.TIME_LIMIT_EXCEEDED, "", "", "", 2000, ""), new ArrayList<>()))
            .judge(CodeLanguage.PYTHON, "x", TESTS, 2, 256);
        assertEquals(CodeVerdict.TIME_LIMIT_EXCEEDED, tle.verdict());
    }

    @Test
    void hiddenCasesNeverRevealInputExpectedOutputActualOutputOrErrors() {
        Function<String, RunOutcome> crashOnHidden = in -> in.trim().equals("3") || in.trim().equals("4")
            ? RunOutcome.of(Status.RUNTIME_ERROR, "partial output with secret 3", "Traceback: bad value 3", "", 4, "")
            : ok(String.valueOf(Integer.parseInt(in.trim()) * 2));
        CodeJudge.Report r = new CodeJudge(runner(crashOnHidden, new ArrayList<>())).judge(CodeLanguage.PYTHON, "x", TESTS, 2, 256);
        for (CodeJudge.CaseResult c : r.cases()) {
            if (c.hidden()) {
                assertNull(c.input());
                assertNull(c.expectedOutput());
                assertNull(c.actualOutput());
                assertNull(c.errorOutput());
            } else {
                assertNotNull(c.input());
                assertNotNull(c.expectedOutput());
            }
        }
        assertEquals(CodeJudge.RUNTIME_ERROR, r.cases().get(2).verdict());
        assertEquals(2, r.passed());
    }

    @Test
    void sandboxFailuresPropagateInsteadOfBeingInventedAsAVerdict() {
        CodeRunner broken = runner(in -> RunOutcome.of(Status.INTERNAL_ERROR, "", "", "", null, "queue full"), new ArrayList<>());
        assertThrows(CodeExecutionException.class, () -> new CodeJudge(broken).judge(CodeLanguage.PYTHON, "x", TESTS, 2, 256));
        CodeRunner unreachable = new CodeRunner() {
            public boolean isConfigured() { return true; }
            public RunOutcome execute(CodeLanguage l, String c, String s, double t, int m) { throw new CodeExecutionException("connection refused"); }
        };
        assertThrows(CodeExecutionException.class, () -> new CodeJudge(unreachable).judge(CodeLanguage.PYTHON, "x", TESTS, 2, 256));
    }

    @Test
    void customRunReturnsTheRealOutputAndNoVerdict() {
        CodeJudge.CustomResult r = new CodeJudge(runner(in -> ok("hello " + in.trim()), new ArrayList<>())).runCustom(CodeLanguage.PYTHON, "x", "world", 2, 256);
        assertEquals("OK", r.status());
        assertEquals("hello world", r.stdout());
    }

    @Test
    void outputComparisonIgnoresLineEndingsTrailingSpacesAndTrailingBlankLines() {
        assertTrue(OutputComparator.matches("1\n2\n", "1\r\n2  \r\n\r\n"));
        assertTrue(OutputComparator.matches("YES", "YES\n"));
        assertFalse(OutputComparator.matches("1 2", "1  2"));       // spaces inside a line matter
        assertFalse(OutputComparator.matches("a\nb", "a\nB"));
        assertFalse(OutputComparator.matches("a\nb", "a"));
        assertTrue(OutputComparator.matches("", ""));
    }

    @Test
    void veryLongOutputIsTruncated() {
        CodeJudge.CustomResult r = new CodeJudge(runner(in -> ok("x".repeat(50_000)), new ArrayList<>())).runCustom(CodeLanguage.PYTHON, "x", "", 2, 256);
        assertTrue(r.stdout().length() < 5000);
        assertTrue(r.stdout().contains("truncated"));
    }

    @Test
    void starterTemplatesExistForEveryLanguageAndAreNotEmpty() {
        for (CodeLanguage l : CodeLanguage.values()) {
            assertFalse(CodingTemplates.starters().get(l).isBlank());
        }
        assertTrue(CodingTemplates.starters().get(CodeLanguage.JAVA).contains("public class Main"));
    }
}
