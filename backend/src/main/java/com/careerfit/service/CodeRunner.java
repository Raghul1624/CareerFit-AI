package com.careerfit.service;

import com.careerfit.entity.CodeLanguage;

/**
 * Runs untrusted source code in an isolated sandbox. The Spring Boot server never executes submitted code itself:
 * the implementation calls OnlineCompiler.io, which executes code in an external sandbox.
 */
public interface CodeRunner {

    enum Status { OK, COMPILATION_ERROR, RUNTIME_ERROR, TIME_LIMIT_EXCEEDED, INTERNAL_ERROR }

    /** Result of one program execution. Outputs are decoded text, never null. */
    record RunOutcome(Status status, String stdout, String stderr, String compileOutput, Integer timeMs, String message) {
        public static RunOutcome of(Status status, String stdout, String stderr, String compileOutput, Integer timeMs, String message) {
            return new RunOutcome(status, nz(stdout), nz(stderr), nz(compileOutput), timeMs, nz(message));
        }

        private static String nz(String s) {
            return s == null ? "" : s;
        }
    }

    /** Thrown when the sandbox cannot be reached or misbehaves. No verdict may be invented in that case. */
    class CodeExecutionException extends RuntimeException {
        public CodeExecutionException(String message) {
            super(message);
        }

        public CodeExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    boolean isConfigured();

    /** @throws CodeExecutionException when the sandbox is unavailable */
    RunOutcome execute(CodeLanguage language, String sourceCode, String stdin, double timeLimitSeconds, int memoryLimitMb);
}
