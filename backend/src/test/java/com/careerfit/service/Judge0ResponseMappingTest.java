package com.careerfit.service;

import com.careerfit.entity.CodeLanguage;
import com.careerfit.service.CodeRunner.RunOutcome;
import com.careerfit.service.CodeRunner.Status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Maps canned OnlineCompiler native responses. No network or API key required. */
class Judge0ResponseMappingTest {
    private static final ObjectMapper OM = new ObjectMapper();

    private static JsonNode json(String status, int exitCode, String output, String error, String time) throws Exception {
        return OM.readTree(OM.writeValueAsString(java.util.Map.of(
                "status", status, "exit_code", exitCode, "output", output, "error", error, "time", time)));
    }

    @Test
    void successfulOutputAndSecondsAreMappedToMilliseconds() throws Exception {
        RunOutcome out = Judge0CodeRunner.map(json("success", 0, "42\n", "", "0.123"), CodeLanguage.JAVA);
        assertEquals(Status.OK, out.status());
        assertEquals("42\n", out.stdout());
        assertEquals(123, out.timeMs());
    }

    @Test
    void javaCompilationErrorsAreExposedAsCompileOutput() throws Exception {
        RunOutcome out = Judge0CodeRunner.map(json("error", 1, "", "Main.java:1: error: ';' expected", "0.02"), CodeLanguage.JAVA);
        assertEquals(Status.COMPILATION_ERROR, out.status());
        assertTrue(out.compileOutput().contains("';' expected"));
    }

    @Test
    void runtimeErrorsAndTimeoutsAreMapped() throws Exception {
        assertEquals(Status.RUNTIME_ERROR, Judge0CodeRunner.map(json("error", 1, "", "ZeroDivisionError", "0.01"), CodeLanguage.PYTHON).status());
        assertEquals(Status.TIME_LIMIT_EXCEEDED, Judge0CodeRunner.map(json("error", 124, "", "timeout", "2.0"), CodeLanguage.PYTHON).status());
    }

    @Test
    void syntaxErrorsAreCompilationErrors() throws Exception {
        assertEquals(Status.COMPILATION_ERROR, Judge0CodeRunner.map(json("error", 1, "", "SyntaxError: invalid syntax", "0.01"), CodeLanguage.PYTHON).status());
    }

    @Test
    void withoutAnApiKeyExecutionIsDisabled() {
        Judge0CodeRunner runner = new Judge0CodeRunner("", "openjdk-25", "python-3.14", "nodejs-24", 1000, 1000);
        assertFalse(runner.isConfigured());
        assertThrows(CodeRunner.CodeExecutionException.class,
                () -> runner.execute(CodeLanguage.PYTHON, "print(1)", "", 2, 256));
        assertTrue(new Judge0CodeRunner("test-key", "openjdk-25", "python-3.14", "nodejs-24", 1000, 1000).isConfigured());
    }
}
