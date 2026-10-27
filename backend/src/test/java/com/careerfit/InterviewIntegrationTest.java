package com.careerfit;

import com.careerfit.service.CodeRunner;
import com.careerfit.entity.CodeLanguage;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Interview Preparation end to end: catalog and availability, the five rounds, scoring on the server, no answer-key
 * leakage, hidden test cases, ownership and authentication.
 *
 * The coding round uses a FAKE sandbox (test-only {@link CodeRunner} bean) so the application's own logic can be tested
 * without a live OnlineCompiler API. These tests prove the plumbing and scoring; they do not prove external execution.
 */
@Import(InterviewIntegrationTest.FakeSandbox.class)
class InterviewIntegrationTest extends IntegrationTestBase {

    static final AtomicBoolean SANDBOX_CONFIGURED = new AtomicBoolean(true);

    @TestConfiguration
    static class FakeSandbox {
        @Bean
        @Primary
        CodeRunner fakeCodeRunner(JdbcTemplate jdbc) {
            return new CodeRunner() {
                private final Pattern marker = Pattern.compile("(CORRECT|PARTIAL):(\\d+)");

                @Override
                public boolean isConfigured() {
                    return SANDBOX_CONFIGURED.get();
                }

                @Override
                public RunOutcome execute(CodeLanguage language, String code, String stdin, double timeLimit, int memoryMb) {
                    if (code.contains("COMPILE_ERROR")) return RunOutcome.of(Status.COMPILATION_ERROR, "", "", "Main.java:1: error: ';' expected", null, "");
                    if (code.contains("RUNTIME_ERROR")) return RunOutcome.of(Status.RUNTIME_ERROR, "", "Exception in thread main", "", 5, "");
                    if (code.contains("ECHO")) return RunOutcome.of(Status.OK, "echo:" + stdin, "", "", 7, "");
                    Matcher m = marker.matcher(code);
                    if (m.find()) {
                        long questionId = Long.parseLong(m.group(2));
                        List<Map<String, Object>> rows = jdbc.queryForList(
                            "select expected_output, hidden from interview_test_cases where question_id = ? and input_data = ?", questionId, stdin);
                        if (!rows.isEmpty()) {
                            boolean hidden = Boolean.TRUE.equals(rows.get(0).get("hidden"));
                            boolean right = m.group(1).equals("CORRECT") || !hidden;       // PARTIAL: right on samples, wrong on hidden cases
                            return RunOutcome.of(Status.OK, right ? String.valueOf(rows.get(0).get("expected_output")) + "\n" : "wrong\n", "", "", 9, "");
                        }
                    }
                    return RunOutcome.of(Status.OK, "wrong\n", "", "", 3, "");
                }
            };
        }
    }

    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void restoreSandbox() {
        SANDBOX_CONFIGURED.set(true);
    }

    // ------------------------------------------------------------------ helpers

    private JsonNode getJson(String url, String token) throws Exception {
        return json(mvc.perform(get(url).header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
    }

    private MvcResult startRaw(String token, String role, String round, String level) throws Exception {
        return mvc.perform(post("/api/interview/attempts").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(Map.of("roleCode", role, "round", round, "level", level)))).andReturn();
    }

    private JsonNode start(String token, String role, String round, String level) throws Exception {
        MvcResult r = startRaw(token, role, round, level);
        assertEquals(201, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        return json(r);
    }

    private List<Long> optionIds(long questionId, boolean correct) {
        return jdbc.queryForList("select id from interview_options where question_id = ? and is_correct = ? order by display_order", Long.class, questionId, correct);
    }

    private void save(String token, long attemptId, List<Map<String, Object>> answers, int expected) throws Exception {
        mvc.perform(put("/api/interview/attempts/" + attemptId + "/answers").header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(Map.of("answers", answers)))).andExpect(status().is(expected));
    }

    private JsonNode submit(String token, long attemptId) throws Exception {
        return json(mvc.perform(post("/api/interview/attempts/" + attemptId + "/submit").header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andReturn());
    }

    private List<Long> questionIds(JsonNode attempt) {
        List<Long> ids = new ArrayList<>();
        attempt.get("questions").forEach(q -> ids.add(q.get("id").asLong()));
        return ids;
    }

    private MvcResult run(String token, long attemptId, long questionId, String code, String stdin) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("language", "PYTHON", "sourceCode", code));
        if (stdin != null) body.put("stdin", stdin);
        return mvc.perform(post("/api/interview/attempts/" + attemptId + "/questions/" + questionId + "/run").header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body))).andReturn();
    }

    private MvcResult submitCode(String token, long attemptId, long questionId, String code) throws Exception {
        return mvc.perform(post("/api/interview/attempts/" + attemptId + "/questions/" + questionId + "/submit-code").header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(Map.of("language", "PYTHON", "sourceCode", code)))).andReturn();
    }

    private boolean questionBelongsToRole(long questionId, String roleCode) {
        return jdbc.queryForObject("select count(*) from interview_question_roles l join interview_roles r on r.id = l.role_id "
            + "where l.question_id = ? and r.code = ?", Integer.class, questionId, roleCode) > 0;
    }

    private static final List<String> ROLES = List.of("JAVA", "PYTHON", "FRONTEND", "FULLSTACK", "QA", "DATA_ANALYST");
    private static final List<String> ROUNDS = List.of("APTITUDE", "TECHNICAL_MCQ", "CODING", "TECHNICAL_INTERVIEW", "HR_INTERVIEW");
    private static final List<String> LEVELS = List.of("EASY", "MEDIUM", "HARD");

    // ------------------------------------------------------------------ catalog and availability

    @Test
    void catalogListsSixRolesWithFiveRoundsAndThreeLevelsAllAvailable() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode roles = getJson("/api/interview/roles", token);
        assertEquals(6, roles.size());
        Set<String> codes = new HashSet<>();
        for (JsonNode role : roles) {
            codes.add(role.get("code").asText());
            assertEquals(5, role.get("rounds").size());
            for (JsonNode round : role.get("rounds")) {
                assertEquals(3, round.get("levels").size());
                for (JsonNode level : round.get("levels")) {
                    assertTrue(level.get("available").asBoolean(), role.get("code").asText() + " " + round.get("round").asText() + " " + level.get("level").asText());
                    assertTrue(level.get("availableQuestions").asInt() >= level.get("questionCount").asInt());
                    assertTrue(level.get("durationMinutes").asInt() > 0);
                }
            }
        }
        assertEquals(new HashSet<>(ROLES), codes);
        assertEquals("JAVA", getJson("/api/interview/roles/java", token).get("code").asText());
        mvc.perform(get("/api/interview/roles/NOPE").header("Authorization", bearer(token))).andExpect(status().isNotFound());
    }

    @Test
    void anAssessmentWithoutEnoughQuestionsIsNotOfferedAndCannotBeStarted() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        jdbc.update("insert into interview_roles (id, code, name, description, display_order, active) values (9001, 'GHOST', 'Ghost Role', 'no questions', 99, true)");
        try {
            JsonNode role = getJson("/api/interview/roles/GHOST", token);
            for (JsonNode round : role.get("rounds")) {
                for (JsonNode level : round.get("levels")) {
                    assertFalse(level.get("available").asBoolean());
                    assertEquals(0, level.get("availableQuestions").asInt());
                    assertFalse(level.get("reason").asText().isBlank());
                }
            }
            MvcResult r = startRaw(token, "GHOST", "APTITUDE", "EASY");
            assertEquals(409, r.getResponse().getStatus());
            assertTrue(r.getResponse().getContentAsString().contains("not available"));
        } finally {
            jdbc.update("delete from interview_roles where id = 9001");
        }
    }

    @Test
    void aQuestionWithoutACorrectOptionIsNeverServed() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        // break one question: remove the correct flag from every option, then check the pool shrinks by exactly that question
        long victim = jdbc.queryForObject("select min(q.id) from interview_questions q join interview_question_roles l on l.question_id = q.id "
            + "where l.role_id = (select id from interview_roles where code = 'JAVA') and q.round_type = 'TECHNICAL_MCQ' and q.difficulty_level = 'HARD'", Long.class);
        int before = availableQuestions(token, "JAVA", "TECHNICAL_MCQ", "HARD");
        List<Long> correct = optionIds(victim, true);
        jdbc.update("update interview_options set is_correct = false where question_id = ?", victim);
        try {
            assertEquals(before - 1, availableQuestions(token, "JAVA", "TECHNICAL_MCQ", "HARD"));
        } finally {
            for (Long id : correct) jdbc.update("update interview_options set is_correct = true where id = ?", id);
        }
        assertEquals(before, availableQuestions(token, "JAVA", "TECHNICAL_MCQ", "HARD"));
    }

    private int availableQuestions(String token, String role, String round, String level) throws Exception {
        for (JsonNode r : getJson("/api/interview/roles/" + role, token).get("rounds")) {
            if (!r.get("round").asText().equals(round)) continue;
            for (JsonNode l : r.get("levels")) if (l.get("level").asText().equals(level)) return l.get("availableQuestions").asInt();
        }
        throw new IllegalStateException("not found");
    }

    // ------------------------------------------------------------------ MCQ rounds

    @Test
    void everyRoleCanStartEveryMultipleChoiceRoundAtEveryLevelWithoutAnswerLeakage() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        for (String role : ROLES) {
            for (String round : List.of("APTITUDE", "TECHNICAL_MCQ")) {
                for (String level : LEVELS) {
                    MvcResult r = startRaw(token, role, round, level);
                    assertEquals(201, r.getResponse().getStatus(), role + " " + round + " " + level);
                    String body = r.getResponse().getContentAsString();
                    assertFalse(body.contains("\"correct\""), "answer key leaked");
                    assertFalse(body.contains("\"isCorrect\""));
                    assertFalse(body.contains("explanation"));
                    JsonNode attempt = json(r);
                    assertEquals(10, attempt.get("questions").size());
                    Set<Long> unique = new HashSet<>(questionIds(attempt));
                    assertEquals(10, unique.size(), "no question twice in one attempt");
                    for (JsonNode q : attempt.get("questions")) {
                        assertTrue(q.get("options").size() >= 2);
                        assertTrue(q.get("coding").isNull());
                    }
                }
            }
        }
    }

    @Test
    void technicalMcqQuestionsBelongToTheSelectedRole() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        for (String role : ROLES) {
            for (String level : LEVELS) {
                for (Long qid : questionIds(start(token, role, "TECHNICAL_MCQ", level))) {
                    assertTrue(questionBelongsToRole(qid, role), "question " + qid + " is not a " + role + " question");
                    assertEquals(level, jdbc.queryForObject("select difficulty_level from interview_questions where id = ?", String.class, qid));
                    assertEquals("TECHNICAL_MCQ", jdbc.queryForObject("select round_type from interview_questions where id = ?", String.class, qid));
                }
            }
        }
    }

    @Test
    void mcqAttemptIsScoredOnTheServerWithExplanationsAfterSubmission() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, "PYTHON", "TECHNICAL_MCQ", "MEDIUM");
        long attemptId = attempt.get("id").asLong();
        List<Long> ids = questionIds(attempt);

        // answers and the key are not available before submitting
        mvc.perform(get("/api/interview/attempts/" + attemptId + "/result").header("Authorization", bearer(token))).andExpect(status().isConflict());

        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            long qid = ids.get(i);
            List<Long> chosen = i < 6 ? optionIds(qid, true) : List.of(optionIds(qid, false).get(0));   // 6 right, 4 wrong
            answers.add(Map.of("questionId", qid, "selectedOptionIds", chosen, "markedForReview", false));
        }
        save(token, attemptId, answers, 204);
        JsonNode result = submit(token, attemptId);

        assertEquals(6.0, result.get("score").asDouble(), 0.001);
        assertEquals(10.0, result.get("maxScore").asDouble(), 0.001);
        assertEquals(60.0, result.get("percentage").asDouble(), 0.001);
        assertEquals(6, result.get("correctCount").asInt());
        assertTrue(result.get("passed").asBoolean());
        assertEquals("PYTHON", result.get("roleCode").asText());
        assertFalse(result.get("summaryFeedback").asText().isBlank());
        assertFalse(result.get("scoringMethod").asText().isBlank());
        assertEquals(10, result.get("items").size());
        for (JsonNode item : result.get("items")) {
            assertFalse(item.get("explanation").asText().isBlank(), "every question needs an explanation");
            boolean anyCorrect = false;
            for (JsonNode o : item.get("options")) anyCorrect |= o.get("correct").asBoolean();
            assertTrue(anyCorrect);
        }
        int topicTotal = 0;
        for (JsonNode t : result.get("topicPerformance")) topicTotal += t.get("total").asInt();
        assertEquals(10, topicTotal);

        // submitting again is harmless and returns the same result
        assertEquals(60.0, submit(token, attemptId).get("percentage").asDouble(), 0.001);
        // saving after submission is rejected
        save(token, attemptId, answers, 409);
    }

    @Test
    void unansweredQuestionsScoreZeroAndAnAttemptCanBeResumedWithItsSavedAnswers() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, "JAVA", "APTITUDE", "EASY");
        long attemptId = attempt.get("id").asLong();
        long first = questionIds(attempt).get(0);
        long chosen = optionIds(first, true).get(0);
        save(token, attemptId, List.of(Map.of("questionId", first, "selectedOptionIds", List.of(chosen), "markedForReview", true)), 204);

        JsonNode resumed = start(token, "JAVA", "APTITUDE", "EASY");           // same role, round and level: resumes
        assertEquals(attemptId, resumed.get("id").asLong());
        JsonNode saved = resumed.get("questions").get(0).get("saved");
        assertEquals(chosen, saved.get("selectedOptionIds").get(0).asLong());
        assertTrue(saved.get("markedForReview").asBoolean());

        JsonNode result = submit(token, attemptId);
        assertEquals(1, result.get("correctCount").asInt());
        assertEquals(10.0, result.get("percentage").asDouble(), 0.001);
        assertFalse(result.get("passed").asBoolean());
    }

    @Test
    void aRetakePrefersQuestionsTheUserHasNotSeen() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode first = start(token, "JAVA", "TECHNICAL_MCQ", "EASY");
        submit(token, first.get("id").asLong());
        JsonNode second = start(token, "JAVA", "TECHNICAL_MCQ", "EASY");
        assertNotEquals(first.get("id").asLong(), second.get("id").asLong());
        Set<Long> overlap = new HashSet<>(questionIds(first));
        overlap.retainAll(questionIds(second));
        assertTrue(overlap.isEmpty(), "the Java Easy technical pool is larger than twice the test size, so a retake need not repeat: " + overlap);
    }

    // ------------------------------------------------------------------ coding round

    @Test
    void codingAttemptNeverExposesHiddenTestCasesOrTheSolution() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        MvcResult r = startRaw(token, "JAVA", "CODING", "EASY");
        assertEquals(201, r.getResponse().getStatus());
        JsonNode attempt = json(r);
        assertEquals(2, attempt.get("questions").size());
        for (JsonNode q : attempt.get("questions")) {
            JsonNode coding = q.get("coding");
            assertFalse(coding.isNull());
            assertFalse(q.get("title").asText().isBlank());
            assertFalse(coding.get("constraints").asText().isBlank());
            assertTrue(coding.get("samples").size() >= 1, "sample test cases are shown");
            assertTrue(coding.get("hiddenTestCount").asInt() >= 3, "hidden cases exist but only their count is returned");
            assertEquals(3, coding.get("languages").size());
            assertTrue(coding.get("starterCode").get("JAVA").asText().contains("public class Main"));
            // none of the hidden inputs/outputs appears anywhere in the payload
            List<Map<String, Object>> hidden = jdbc.queryForList("select input_data, expected_output from interview_test_cases where question_id = ? and hidden = true", q.get("id").asLong());
            String payload = q.toString();
            for (Map<String, Object> h : hidden) {
                String in = String.valueOf(h.get("input_data")).trim(), out = String.valueOf(h.get("expected_output")).trim();
                if (in.length() > 12) assertFalse(payload.contains(om.writeValueAsString(in).replaceAll("^\"|\"$", "")), "hidden input leaked");
                if (out.length() > 12) assertFalse(payload.contains(om.writeValueAsString(out).replaceAll("^\"|\"$", "")), "hidden output leaked");
            }
        }
        String body = r.getResponse().getContentAsString();
        assertFalse(body.contains("\"hidden\":true"));
        assertFalse(body.contains("expectedOutput\":null"));
    }

    @Test
    void runExecutesSamplesAndCustomInputInTheSandboxAndReportsRealResults() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, "PYTHON", "CODING", "EASY");
        long attemptId = attempt.get("id").asLong();
        long qid = questionIds(attempt).get(0);
        int samples = attempt.get("questions").get(0).get("coding").get("samples").size();

        JsonNode ok = json(run(token, attemptId, qid, "CORRECT:" + qid, null));
        assertFalse(ok.get("custom").asBoolean());
        assertEquals(samples, ok.get("cases").size(), "Run executes the sample cases only");
        assertEquals(samples, ok.get("passed").asInt());
        for (JsonNode c : ok.get("cases")) {
            assertEquals("PASSED", c.get("verdict").asText());
            assertFalse(c.get("hidden").asBoolean());
            assertFalse(c.get("input").isNull());
        }

        JsonNode wrong = json(run(token, attemptId, qid, "WRONG", null));
        assertEquals("WRONG_ANSWER", wrong.get("cases").get(0).get("verdict").asText());
        assertEquals("wrong", wrong.get("cases").get(0).get("actualOutput").asText().trim());

        JsonNode custom = json(run(token, attemptId, qid, "ECHO", "hello"));
        assertTrue(custom.get("custom").asBoolean());
        assertEquals("OK", custom.get("status").asText());
        assertEquals("echo:hello", custom.get("stdout").asText());

        JsonNode compile = json(run(token, attemptId, qid, "COMPILE_ERROR", null));
        assertEquals("COMPILATION_ERROR", compile.get("status").asText());
        assertTrue(compile.get("compileOutput").asText().contains("expected"));

        JsonNode runtime = json(run(token, attemptId, qid, "RUNTIME_ERROR", null));
        assertEquals("RUNTIME_ERROR", runtime.get("cases").get(0).get("verdict").asText());
        assertTrue(runtime.get("cases").get(0).get("errorOutput").asText().contains("Exception"));
    }

    @Test
    void submissionsAreJudgedOnHiddenCasesScoredAndSaved() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, "JAVA", "CODING", "MEDIUM");
        long attemptId = attempt.get("id").asLong();
        long q1 = questionIds(attempt).get(0), q2 = questionIds(attempt).get(1);
        int total1 = jdbc.queryForObject("select count(*) from interview_test_cases where question_id = ?", Integer.class, q1);
        int samples1 = jdbc.queryForObject("select count(*) from interview_test_cases where question_id = ? and hidden = false", Integer.class, q1);

        // 1) passes the samples only: partial credit
        JsonNode partial = json(submitCode(token, attemptId, q1, "PARTIAL:" + q1));
        assertEquals("PARTIAL", partial.get("verdict").asText());
        assertEquals(samples1, partial.get("passedTests").asInt());
        assertEquals(total1, partial.get("totalTests").asInt());
        double partialScore = Math.round(samples1 * 10000.0 / total1) / 100.0;
        assertEquals(partialScore, partial.get("scorePercent").asDouble(), 0.001);
        int hiddenSeen = 0;
        for (JsonNode c : partial.get("cases")) {
            if (c.get("hidden").asBoolean()) {
                hiddenSeen++;
                assertTrue(c.get("input").isNull() && c.get("expectedOutput").isNull() && c.get("actualOutput").isNull());
                assertEquals("WRONG_ANSWER", c.get("verdict").asText());
            }
        }
        assertEquals(total1 - samples1, hiddenSeen);

        // 2) a worse later submission does not lower the score: the best submission counts
        assertEquals("WRONG_ANSWER", json(submitCode(token, attemptId, q1, "WRONG")).get("verdict").asText());
        assertEquals("COMPILATION_ERROR", json(submitCode(token, attemptId, q1, "COMPILE_ERROR")).get("verdict").asText());

        // 3) the second problem is solved completely
        JsonNode accepted = json(submitCode(token, attemptId, q2, "CORRECT:" + q2));
        assertEquals("ACCEPTED", accepted.get("verdict").asText());
        assertEquals(100.0, accepted.get("scorePercent").asDouble(), 0.001);

        // submissions are saved and listed newest first
        JsonNode list = getJson("/api/interview/attempts/" + attemptId + "/questions/" + q1 + "/submissions", token);
        assertEquals(4, list.size());
        assertEquals("COMPILATION_ERROR", list.get(0).get("verdict").asText());
        assertFalse(list.get(3).get("sourceCode").asText().isBlank());

        JsonNode result = submit(token, attemptId);
        double expected = (partialScore + 100.0) / 200.0 * 100.0;
        assertEquals(Math.round(expected * 100) / 100.0, result.get("percentage").asDouble(), 0.01);
        JsonNode firstItem = result.get("items").get(0);
        assertEquals(4, firstItem.get("coding").get("submissionCount").asInt());
        assertEquals(partialScore, firstItem.get("coding").get("bestScorePercent").asDouble(), 0.001);
        assertEquals(partialScore, firstItem.get("score").asDouble(), 0.001);
    }

    @Test
    void codingEndpointsFailClearlyWhenNoSandboxIsConfiguredAndNeverInventResults() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, "QA", "CODING", "EASY");
        long attemptId = attempt.get("id").asLong();
        long qid = questionIds(attempt).get(0);
        SANDBOX_CONFIGURED.set(false);

        assertFalse(getJson("/api/interview/coding/status", token).get("configured").asBoolean());
        MvcResult run = run(token, attemptId, qid, "CORRECT:" + qid, null);
        assertEquals(503, run.getResponse().getStatus());
        assertTrue(run.getResponse().getContentAsString().contains("ONLINECOMPILER_API_KEY"));
        assertEquals(503, submitCode(token, attemptId, qid, "CORRECT:" + qid).getResponse().getStatus());
        assertEquals(0, jdbc.queryForObject("select count(*) from code_submissions where attempt_item_id in "
            + "(select id from interview_attempt_items where attempt_id = ?)", Integer.class, attemptId),
            "no submission may be saved without a real execution");
        assertEquals(0.0, submit(token, attemptId).get("percentage").asDouble(), 0.001);
    }

    @Test
    void codingEndpointsRejectBadInputAndWrongRounds() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode coding = start(token, "FRONTEND", "CODING", "HARD");
        long attemptId = coding.get("id").asLong();
        long qid = questionIds(coding).get(0);
        // a question that is not part of the attempt
        assertEquals(404, run(token, attemptId, 999999L, "x", null).getResponse().getStatus());
        // blank code, unsupported language, oversized code
        assertEquals(400, mvc.perform(post("/api/interview/attempts/" + attemptId + "/questions/" + qid + "/run").header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"PYTHON\",\"sourceCode\":\"\"}")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(post("/api/interview/attempts/" + attemptId + "/questions/" + qid + "/run").header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"COBOL\",\"sourceCode\":\"x\"}")).andReturn().getResponse().getStatus());
        assertEquals(400, run(token, attemptId, qid, "x".repeat(10001), null).getResponse().getStatus());
        // a coding endpoint on an MCQ attempt
        JsonNode mcq = start(token, "FRONTEND", "APTITUDE", "EASY");
        assertEquals(400, run(token, mcq.get("id").asLong(), questionIds(mcq).get(0), "x", null).getResponse().getStatus());
    }

    // ------------------------------------------------------------------ interview rounds (rubric)

    @Test
    void technicalInterviewAnswersAreEvaluatedWithARubricAndTheModelAnswerIsRevealedAfterwards() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, "JAVA", "TECHNICAL_INTERVIEW", "EASY");
        long attemptId = attempt.get("id").asLong();
        assertEquals(5, attempt.get("questions").size());
        String payload = attempt.toString();
        assertFalse(payload.contains("keywords"));
        assertFalse(payload.contains("explanation"));
        for (JsonNode q : attempt.get("questions")) {
            assertEquals("TEXT", q.get("type").asText());
            assertTrue(q.get("options").isNull());
        }

        List<Long> ids = questionIds(attempt);
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            String modelAnswer = jdbc.queryForObject("select explanation from interview_questions where id = ?", String.class, ids.get(i));
            answers.add(Map.of("questionId", ids.get(i), "answerText", i == 0 ? "I do not know." : modelAnswer, "markedForReview", false));
        }
        save(token, attemptId, answers, 204);
        JsonNode result = submit(token, attemptId);

        assertEquals(5, result.get("items").size());
        double weakest = 100, strongest = 0;
        for (JsonNode item : result.get("items")) {
            JsonNode ev = item.get("evaluation");
            assertEquals(4, ev.get("dimensions").size());
            assertEquals(100, sumWeights(ev));
            assertFalse(ev.get("strengths").isEmpty());
            assertFalse(item.get("explanation").asText().isBlank(), "the model answer is shown after submission");
            double score = item.get("score").asDouble();
            assertEquals(ev.get("overall").asDouble(), score, 0.001);
            weakest = Math.min(weakest, score);
            strongest = Math.max(strongest, score);
        }
        assertTrue(weakest <= 10, "an 'I do not know' answer scores almost nothing: " + weakest);
        assertTrue(strongest >= 70, "a model answer scores high: " + strongest);
        assertTrue(result.get("percentage").asDouble() > 40);
        assertTrue(result.get("scoringMethod").asText().contains("no AI"));
    }

    private int sumWeights(JsonNode evaluation) {
        int sum = 0;
        for (JsonNode d : evaluation.get("dimensions")) sum += d.get("weightPercent").asInt();
        return sum;
    }

    @Test
    void hrInterviewIsEvaluatedAndGivesImprovementSuggestions() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, "DATA_ANALYST", "HR_INTERVIEW", "MEDIUM");
        long attemptId = attempt.get("id").asLong();
        assertEquals(6, attempt.get("questions").size());
        for (JsonNode q : attempt.get("questions")) assertFalse(q.get("guidance").asText().isBlank(), "HR questions show guidance while answering");

        List<Map<String, Object>> answers = new ArrayList<>();
        for (Long qid : questionIds(attempt)) {
            answers.add(Map.of("questionId", qid, "answerText", "I am a hard worker and I always do my best for the team.", "markedForReview", false));
        }
        save(token, attemptId, answers, 204);
        JsonNode result = submit(token, attemptId);
        for (JsonNode item : result.get("items")) {
            JsonNode ev = item.get("evaluation");
            assertEquals("Relevance", ev.get("dimensions").get(0).get("name").asText());
            assertFalse(ev.get("improvements").isEmpty(), "a vague answer must come with improvement suggestions");
            assertTrue(item.get("score").asDouble() < 50);
        }
        assertFalse(result.get("passed").asBoolean());
        assertTrue(result.get("summaryFeedback").asText().contains("STAR"));
    }

    @Test
    void textAnswersAreValidated() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, "QA", "TECHNICAL_INTERVIEW", "MEDIUM");
        long qid = questionIds(attempt).get(0);
        save(token, attempt.get("id").asLong(), List.of(Map.of("questionId", qid, "answerText", "a".repeat(5001))), 400);
        save(token, attempt.get("id").asLong(), List.of(Map.of("questionId", qid, "answerText", "a".repeat(10001))), 400);
        save(token, attempt.get("id").asLong(), List.of(Map.of("questionId", 424242L, "answerText", "x")), 400);
    }

    // ------------------------------------------------------------------ validation, ownership, authentication, history

    @Test
    void invalidRequestsAreRejected() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        assertEquals(404, startRaw(token, "UNKNOWN_ROLE", "APTITUDE", "EASY").getResponse().getStatus());
        assertEquals(400, startRaw(token, "JAVA", "NOT_A_ROUND", "EASY").getResponse().getStatus());
        assertEquals(400, startRaw(token, "JAVA", "APTITUDE", "IMPOSSIBLE").getResponse().getStatus());
        assertEquals(400, mvc.perform(post("/api/interview/attempts").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andReturn().getResponse().getStatus());

        JsonNode attempt = start(token, "JAVA", "APTITUDE", "MEDIUM");
        long attemptId = attempt.get("id").asLong();
        long q0 = questionIds(attempt).get(0), q1 = questionIds(attempt).get(1);
        save(token, attemptId, List.of(Map.of("questionId", q0, "selectedOptionIds", List.of(987654321L))), 400);             // option does not exist
        save(token, attemptId, List.of(Map.of("questionId", q0, "selectedOptionIds", List.of(optionIds(q1, true).get(0)))), 400); // option of another question
        List<Long> two = jdbc.queryForList("select id from interview_options where question_id = ?", Long.class, q0);
        String type = jdbc.queryForObject("select question_type from interview_questions where id = ?", String.class, q0);
        if (type.equals("SINGLE_CHOICE")) save(token, attemptId, List.of(Map.of("questionId", q0, "selectedOptionIds", List.of(two.get(0), two.get(1)))), 400);
        save(token, attemptId, List.of(Map.of("questionId", q0, "selectedOptionIds", java.util.stream.LongStream.rangeClosed(1, 11).boxed().toList())), 400);
        mvc.perform(put("/api/interview/attempts/" + attemptId + "/answers").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        // a rejected save must not have changed anything
        assertTrue(getJson("/api/interview/attempts/" + attemptId, token).get("questions").get(0).get("saved").get("selectedOptionIds").isEmpty());
    }

    @Test
    void usersCannotSeeOrChangeEachOthersAttempts() throws Exception {
        String owner = registerAndGetToken(uniqueEmail());
        String other = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(owner, "PYTHON", "CODING", "EASY");
        long attemptId = attempt.get("id").asLong();
        long qid = questionIds(attempt).get(0);

        mvc.perform(get("/api/interview/attempts/" + attemptId).header("Authorization", bearer(other))).andExpect(status().isNotFound());
        mvc.perform(get("/api/interview/attempts/" + attemptId + "/result").header("Authorization", bearer(other))).andExpect(status().isNotFound());
        mvc.perform(post("/api/interview/attempts/" + attemptId + "/submit").header("Authorization", bearer(other))).andExpect(status().isNotFound());
        save(other, attemptId, List.of(Map.of("questionId", qid, "answerText", "x")), 404);
        assertEquals(404, run(other, attemptId, qid, "CORRECT:" + qid, null).getResponse().getStatus());
        assertEquals(404, submitCode(other, attemptId, qid, "CORRECT:" + qid).getResponse().getStatus());
        mvc.perform(get("/api/interview/attempts/" + attemptId + "/questions/" + qid + "/submissions").header("Authorization", bearer(other))).andExpect(status().isNotFound());

        // the owner is unaffected and the other user's history is empty
        assertEquals(200, mvc.perform(get("/api/interview/attempts/" + attemptId).header("Authorization", bearer(owner))).andReturn().getResponse().getStatus());
        assertEquals(0, getJson("/api/interview/attempts/history", other).get("totalElements").asInt());
        assertEquals(0, getJson("/api/interview/overview", other).get("totalAttempts").asInt());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/interview/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/interview/roles/JAVA")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/interview/overview")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/interview/attempts").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/interview/attempts/history")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/interview/attempts/1")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/interview/attempts/1/answers").contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[]}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/interview/attempts/1/submit")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/interview/attempts/1/result")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/interview/coding/status")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/interview/attempts/1/questions/1/run").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/interview/attempts/1/questions/1/submit-code").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/interview/attempts/1/questions/1/submissions")).andExpect(status().isUnauthorized());
    }

    @Test
    void historyListsOnlyTheUsersAttemptsAndSupportsFilters() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        submit(token, start(token, "JAVA", "APTITUDE", "EASY").get("id").asLong());
        submit(token, start(token, "JAVA", "TECHNICAL_MCQ", "EASY").get("id").asLong());
        start(token, "QA", "APTITUDE", "EASY");                                          // left in progress

        JsonNode all = getJson("/api/interview/attempts/history", token);
        assertEquals(3, all.get("totalElements").asInt());
        JsonNode first = all.get("content").get(0);
        assertEquals("QA", first.get("roleCode").asText());
        assertEquals("IN_PROGRESS", first.get("status").asText());
        assertEquals("Aptitude Round", first.get("roundLabel").asText());

        assertEquals(2, getJson("/api/interview/attempts/history?role=JAVA", token).get("totalElements").asInt());
        assertEquals(2, getJson("/api/interview/attempts/history?round=APTITUDE", token).get("totalElements").asInt());
        assertEquals(1, getJson("/api/interview/attempts/history?role=JAVA&round=TECHNICAL_MCQ", token).get("totalElements").asInt());
        assertEquals(3, getJson("/api/interview/overview", token).get("totalAttempts").asInt());
        assertEquals(3, getJson("/api/interview/overview", token).get("recent").size());
    }

    @Test
    void anExpiredAttemptIsAutoSubmittedAndTheSubmissionIsKept() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, "FULLSTACK", "APTITUDE", "EASY");
        long attemptId = attempt.get("id").asLong();
        long qid = questionIds(attempt).get(0);
        // hibernate.jdbc.time_zone is UTC, so the stored wall-clock time is UTC: bind it explicitly instead of using the DB's local clock
        jdbc.update("update interview_attempts set expires_at = ? where id = ?",
            java.time.LocalDateTime.ofInstant(java.time.Instant.now().minusSeconds(600), java.time.ZoneOffset.UTC), attemptId);

        save(token, attemptId, List.of(Map.of("questionId", qid, "selectedOptionIds", List.of(optionIds(qid, true).get(0)))), 409);
        assertEquals("AUTO_SUBMITTED", jdbc.queryForObject("select status from interview_attempts where id = ?", String.class, attemptId),
            "the auto-submission must be committed even though the request was rejected");
        JsonNode result = json(mvc.perform(get("/api/interview/attempts/" + attemptId + "/result").header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
        assertEquals("AUTO_SUBMITTED", result.get("status").asText());
        assertEquals(0, result.get("correctCount").asInt(), "answers sent after the deadline are not accepted");
    }

    @Test
    void existingMockTestsStillWorkAlongsideTheInterviewSystem() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        assertEquals(46, getJson("/api/mock-tests", token).size());
        assertEquals(20, getJson("/api/career-paths", token).size());
        MvcResult r = mvc.perform(post("/api/mock-tests/1/attempts").header("Authorization", bearer(token))).andReturn();
        assertEquals(201, r.getResponse().getStatus());
    }

    @Test
    void questionBankIsConsistent() {
        assertEquals(6, jdbc.queryForObject("select count(*) from interview_roles", Integer.class));
        assertEquals(15, jdbc.queryForObject("select count(*) from interview_round_configs", Integer.class));
        List<String> texts = jdbc.queryForList("select round_type || '|' || question_text from interview_questions", String.class);
        assertEquals(texts.size(), new HashSet<>(texts).size(), "no duplicate questions in a round");
        assertEquals(0, jdbc.queryForObject("""
            select count(*) from interview_questions q where q.question_type = 'SINGLE_CHOICE'
              and (select count(*) from interview_options o where o.question_id = q.id and o.is_correct = true) <> 1""", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from interview_questions where round_type = 'CODING' and id not in "
            + "(select question_id from interview_test_cases where hidden = true)", Integer.class), "every coding problem has hidden cases");
        for (String round : ROUNDS) {
            assertTrue(jdbc.queryForObject("select count(*) from interview_questions where round_type = ?", Integer.class, round) > 0, round);
        }
    }
}
