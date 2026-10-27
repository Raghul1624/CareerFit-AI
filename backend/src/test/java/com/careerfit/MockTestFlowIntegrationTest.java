package com.careerfit;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MockTestFlowIntegrationTest extends IntegrationTestBase {

    private static final long JAVA_BASICS_TEST_ID = 1L;

    @Autowired JdbcTemplate jdbc;

    /** Configured size of the Java Basics skill test (read from the data, not hard-coded). */
    private int javaBasicsQuestionCount() {
        return jdbc.queryForObject("select question_count from mock_tests where id = ?", Integer.class, JAVA_BASICS_TEST_ID);
    }

    private JsonNode startAttempt(String token) throws Exception {
        MvcResult r = mvc.perform(post("/api/mock-tests/" + JAVA_BASICS_TEST_ID + "/attempts")
                .header("Authorization", bearer(token)))
            .andExpect(status().isCreated()).andReturn();
        return json(r);
    }

    @Test
    void answerKeyIsNeverSentBeforeSubmission() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        MvcResult r = mvc.perform(post("/api/mock-tests/" + JAVA_BASICS_TEST_ID + "/attempts")
                .header("Authorization", bearer(token)))
            .andExpect(status().isCreated()).andReturn();
        String body = r.getResponse().getContentAsString();
        assertFalse(body.contains("\"correct\""));
        assertFalse(body.contains("explanation"));
        assertEquals(javaBasicsQuestionCount(), json(r).get("questions").size());
    }

    @Test
    void serverScoresSubmittedAnswersAndBlocksDuplicateSubmit() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = startAttempt(token);
        long attemptId = attempt.get("id").asLong();

        // Answer the first question correctly, using the database as the answer key.
        JsonNode first = attempt.get("questions").get(0);
        long qid = first.get("id").asLong();
        List<Long> correctIds = jdbc.queryForList(
            "select id from question_options where question_id = ? and is_correct = true", Long.class, qid);
        String answers = om.writeValueAsString(Map.of("answers", List.of(Map.of(
            "questionId", qid, "selectedOptionIds", correctIds, "markedForReview", false))));
        mvc.perform(put("/api/attempts/" + attemptId + "/answers").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(answers))
            .andExpect(status().isNoContent());

        MvcResult submit = mvc.perform(post("/api/attempts/" + attemptId + "/submit").header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andReturn();
        JsonNode result = json(submit);
        assertEquals(1, result.get("correctCount").asInt());
        assertEquals(0, result.get("incorrectCount").asInt());
        int total = javaBasicsQuestionCount();
        assertEquals(total - 1, result.get("unansweredCount").asInt());
        assertEquals(1.0, result.get("score").asDouble(), 0.001);
        assertEquals(100.0 / total, result.get("percentage").asDouble(), 0.01);

        mvc.perform(post("/api/attempts/" + attemptId + "/submit").header("Authorization", bearer(token)))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/attempts/" + attemptId + "/answers").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(answers))
            .andExpect(status().isConflict());
    }

    @Test
    void wrongAnswersUseNegativeMarking() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = startAttempt(token);
        long attemptId = attempt.get("id").asLong();
        JsonNode first = attempt.get("questions").get(0);
        long qid = first.get("id").asLong();
        List<Long> wrong = new ArrayList<>(jdbc.queryForList(
            "select id from question_options where question_id = ? and is_correct = false", Long.class, qid));
        String answers = om.writeValueAsString(Map.of("answers", List.of(Map.of(
            "questionId", qid, "selectedOptionIds", List.of(wrong.get(0)), "markedForReview", true))));
        mvc.perform(put("/api/attempts/" + attemptId + "/answers").header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON).content(answers)).andExpect(status().isNoContent());
        JsonNode result = json(mvc.perform(post("/api/attempts/" + attemptId + "/submit")
            .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
        assertEquals(1, result.get("incorrectCount").asInt());
        assertEquals(0.0, result.get("score").asDouble(), 0.001);   // score is floored at 0
    }

    @Test
    void usersCannotAccessEachOthersAttempts() throws Exception {
        String owner = registerAndGetToken(uniqueEmail());
        String intruder = registerAndGetToken(uniqueEmail());
        long attemptId = startAttempt(owner).get("id").asLong();

        mvc.perform(get("/api/attempts/" + attemptId).header("Authorization", bearer(intruder)))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/attempts/" + attemptId + "/submit").header("Authorization", bearer(intruder)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/attempts/" + attemptId + "/result").header("Authorization", bearer(intruder)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/attempts/" + attemptId).header("Authorization", bearer(owner)))
            .andExpect(status().isOk());
    }

    @Test
    void resultIsNotAvailableUntilSubmitted() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long attemptId = startAttempt(token).get("id").asLong();
        mvc.perform(get("/api/attempts/" + attemptId + "/result").header("Authorization", bearer(token)))
            .andExpect(status().isConflict());
    }

    @Test
    void startingTwiceResumesTheSameAttemptWithSameQuestionOrder() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode a = startAttempt(token);
        JsonNode b = startAttempt(token);
        assertEquals(a.get("id").asLong(), b.get("id").asLong());
        assertEquals(a.get("questions").toString(), b.get("questions").toString());
    }

    @Test
    void historyAndDashboardReflectRealAttempts() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long attemptId = startAttempt(token).get("id").asLong();
        mvc.perform(post("/api/attempts/" + attemptId + "/submit").header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        JsonNode history = json(mvc.perform(get("/api/attempts/history").header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andReturn());
        assertEquals(1, history.get("totalElements").asInt());

        JsonNode dash = json(mvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andReturn());
        assertEquals(1, dash.get("testsCompleted").asInt());

        // A different user sees an empty history.
        String other = registerAndGetToken(uniqueEmail());
        JsonNode otherHistory = json(mvc.perform(get("/api/attempts/history").header("Authorization", bearer(other)))
            .andExpect(status().isOk()).andReturn());
        assertEquals(0, otherHistory.get("totalElements").asInt());
    }
}
