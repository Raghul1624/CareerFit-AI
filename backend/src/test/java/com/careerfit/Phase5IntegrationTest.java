package com.careerfit;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Phase5IntegrationTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbc;

    private JsonNode getJson(String url, String token) throws Exception {
        return json(mvc.perform(get(url).header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
    }

    private void putJson(String url, String token, Object body, int expected) throws Exception {
        mvc.perform(put(url).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(body))).andExpect(status().is(expected));
    }

    @Test
    void roadmapProgressIsPerUser() throws Exception {
        String a = registerAndGetToken(uniqueEmail());
        String b = registerAndGetToken(uniqueEmail());
        JsonNode list = getJson("/api/learning-roadmaps", a);
        assertEquals(5, list.size());   // Java, Python, Full Stack, Data Analyst, Automation Testing
        long roadmapId = list.get(0).get("id").asLong();
        JsonNode detail = getJson("/api/learning-roadmaps/" + roadmapId, a);
        long itemId = detail.get("items").get(0).get("id").asLong();

        putJson("/api/learning-progress/" + itemId, a, Map.of("completed", true), 200);
        assertEquals(1, getJson("/api/learning-roadmaps/" + roadmapId, a).get("completedSteps").asInt());
        assertEquals(0, getJson("/api/learning-roadmaps/" + roadmapId, b).get("completedSteps").asInt());

        putJson("/api/learning-progress/" + itemId, a, Map.of("completed", false), 200);
        assertEquals(0, getJson("/api/learning-roadmaps/" + roadmapId, a).get("completedSteps").asInt());
        putJson("/api/learning-progress/999999", a, Map.of("completed", true), 404);
    }

    @Test
    void skillGapWithoutTargetCareerIsEmpty() throws Exception {
        String t = registerAndGetToken(uniqueEmail());
        JsonNode gap = getJson("/api/analytics/skill-gap", t);
        assertEquals(0, gap.get("items").size());
    }

    @Test
    void skillGapReflectsDeclaredSkillsAndTestResults() throws Exception {
        String t = registerAndGetToken(uniqueEmail());
        putJson("/api/profile/target-career", t, Map.of("careerPathId", 1), 200);   // Java Developer
        putJson("/api/profile/skills", t, Map.of("skillIds", List.of(1)), 200);     // declares Java Basics

        assertEquals("UNVERIFIED", gapStatus(getJson("/api/analytics/skill-gap", t), "Java Basics"));
        assertEquals("MISSING", gapStatus(getJson("/api/analytics/skill-gap", t), "SQL Basics"));

        // Take the Java Basics test and answer every question correctly.
        JsonNode attempt = json(mvc.perform(post("/api/mock-tests/1/attempts").header("Authorization", bearer(t)))
            .andExpect(status().isCreated()).andReturn());
        List<Map<String, Object>> answers = new java.util.ArrayList<>();
        for (JsonNode q : attempt.get("questions")) {
            long qid = q.get("id").asLong();
            List<Long> ok = jdbc.queryForList("select id from question_options where question_id = ? and is_correct = true", Long.class, qid);
            answers.add(Map.of("questionId", qid, "selectedOptionIds", ok, "markedForReview", false));
        }
        long attemptId = attempt.get("id").asLong();
        putJson("/api/attempts/" + attemptId + "/answers", t, Map.of("answers", answers), 204);
        mvc.perform(post("/api/attempts/" + attemptId + "/submit").header("Authorization", bearer(t))).andExpect(status().isOk());

        JsonNode gap = getJson("/api/analytics/skill-gap", t);
        assertEquals("STRONG", gapStatus(gap, "Java Basics"));
        assertFalse(gap.get("suggestedOrder").toString().contains("Java Basics"));
        assertEquals(1, getJson("/api/analytics/performance", t).get("trend").size());
    }

    @Test
    void unknownSkillIdsAreRejected() throws Exception {
        String t = registerAndGetToken(uniqueEmail());
        putJson("/api/profile/skills", t, Map.of("skillIds", List.of(999999)), 400);
    }

    private static String gapStatus(JsonNode gap, String skill) {
        for (JsonNode i : gap.get("items")) {
            if (skill.equals(i.get("skill").asText())) return i.get("status").asText();
        }
        return "ABSENT";
    }
}
