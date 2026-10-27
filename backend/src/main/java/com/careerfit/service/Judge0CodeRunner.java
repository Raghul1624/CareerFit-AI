package com.careerfit.service;

import com.careerfit.entity.CodeLanguage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Executes submitted code in OnlineCompiler.io's external isolated sandbox. The Spring Boot process
 * never executes user-supplied source code. Configure ONLINECOMPILER_API_KEY to enable execution.
 */
@Service
public class Judge0CodeRunner implements CodeRunner {

    private static final String DEFAULT_BASE_URL = "https://api.onlinecompiler.io";
    private static final int MAX_TIME_LIMIT_SECONDS = 30;

    private final String apiKey;
    private final RestClient client;
    private final Map<CodeLanguage, String> compilerIds = new EnumMap<>(CodeLanguage.class);

    public Judge0CodeRunner(
            @Value("${app.coding.onlinecompiler.api-key:}") String apiKey,
            @Value("${app.coding.onlinecompiler.java-compiler:openjdk-25}") String javaCompiler,
            @Value("${app.coding.onlinecompiler.python-compiler:python-3.14}") String pythonCompiler,
            @Value("${app.coding.onlinecompiler.javascript-compiler:nodejs-24}") String javascriptCompiler,
            @Value("${app.coding.onlinecompiler.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.coding.onlinecompiler.read-timeout-ms:40000}") int readTimeoutMs) {
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        compilerIds.put(CodeLanguage.JAVA, javaCompiler);
        compilerIds.put(CodeLanguage.PYTHON, pythonCompiler);
        compilerIds.put(CodeLanguage.JAVASCRIPT, javascriptCompiler);

        if (this.apiKey.isBlank()) {
            this.client = null;
        } else {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(connectTimeoutMs);
            factory.setReadTimeout(readTimeoutMs);
            this.client = RestClient.builder()
                    .baseUrl(DEFAULT_BASE_URL)
                    .requestFactory(factory)
                    .defaultHeader("Authorization", this.apiKey)
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }
    }

    @Override
    public boolean isConfigured() {
        return client != null;
    }

    @Override
    public RunOutcome execute(CodeLanguage language, String sourceCode, String stdin,
                              double timeLimitSeconds, int memoryLimitMb) {
        if (client == null) {
            throw new CodeExecutionException(
                    "Code execution is not configured. Set ONLINECOMPILER_API_KEY in your backend environment.");
        }
        String compiler = compilerIds.get(language);
        if (compiler == null || compiler.isBlank()) {
            throw new CodeExecutionException("The selected programming language is not configured.");
        }

        // OnlineCompiler accepts a maximum of 30 seconds per run. Its sandbox applies its own memory limit.
        double safeTimeLimit = Math.max(0.1, Math.min(MAX_TIME_LIMIT_SECONDS, timeLimitSeconds));
        Map<String, Object> body = Map.of(
                "compiler", compiler,
                "code", sourceCode,
                "input", stdin == null ? "" : stdin,
                "time_limit", safeTimeLimit
        );

        try {
            JsonNode response = client.post()
                    .uri("/api/run-code-sync/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return map(response, language);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String message;
            if (status == 401 || status == 403) {
                message = "OnlineCompiler rejected the API key (HTTP " + status + "). Check ONLINECOMPILER_API_KEY.";
            } else if (status == 429) {
                message = "OnlineCompiler rate or usage limit reached (HTTP 429). Check your API key usage and monthly limit.";
            } else if (status == 400) {
                message = "OnlineCompiler rejected the code execution request (HTTP 400). Check the code and compiler configuration.";
            } else if (status == 503) {
                message = "OnlineCompiler execution service is temporarily unavailable. Try again shortly.";
            } else {
                message = "OnlineCompiler returned HTTP " + status + ".";
            }
            throw new CodeExecutionException(message, e);
        } catch (ResourceAccessException e) {
            throw new CodeExecutionException("OnlineCompiler could not be reached or the run timed out. Please try again.", e);
        }
    }

    /** Maps OnlineCompiler's native synchronous response into the application's existing verdict model. */
    static RunOutcome map(JsonNode response, CodeLanguage language) {
        if (response == null || response.isNull()) {
            throw new CodeExecutionException("OnlineCompiler returned an empty response.");
        }

        String output = text(response, "output");
        String error = text(response, "error");
        String status = text(response, "status").toLowerCase(Locale.ROOT);
        JsonNode exitNode = response.path("exit_code");
        if (!exitNode.canConvertToInt() || exitNode.isNull()) {
            throw new CodeExecutionException("OnlineCompiler returned a response without an exit code.");
        }
        int exitCode = exitNode.asInt();
        Integer timeMs = parseTimeMs(response.path("time"));

        if (exitCode == 124) {
            return RunOutcome.of(Status.TIME_LIMIT_EXCEEDED, output, error, "", timeMs,
                    "The program exceeded its execution time limit.");
        }
        if (exitCode == 0 && "success".equals(status)) {
            return RunOutcome.of(Status.OK, output, error, "", timeMs, "");
        }
        if (looksLikeCompilationError(error, language)) {
            return RunOutcome.of(Status.COMPILATION_ERROR, output, "", error, timeMs, error);
        }
        if ("error".equals(status) || exitCode != 0) {
            return RunOutcome.of(Status.RUNTIME_ERROR, output, error, "", timeMs,
                    error.isBlank() ? "Program exited with code " + exitCode : error);
        }
        return RunOutcome.of(Status.INTERNAL_ERROR, output, error, "", timeMs,
                "Unexpected response from OnlineCompiler.");
    }

    private static boolean looksLikeCompilationError(String error, CodeLanguage language) {
        if (error == null || error.isBlank()) return false;
        String e = error.toLowerCase(Locale.ROOT);
        return switch (language) {
            case JAVA -> e.contains("error:") || e.contains("compilation failed") || e.contains("cannot find symbol")
                    || e.contains("reached end of file while parsing") || e.contains("';' expected");
            case PYTHON -> e.contains("syntaxerror") || e.contains("indentationerror") || e.contains("taberror");
            case JAVASCRIPT -> e.contains("syntaxerror") || e.contains("unexpected token") || e.contains("is not a valid");
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private static Integer parseTimeMs(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        try {
            return (int) Math.round(Double.parseDouble(node.asText()) * 1000.0);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
