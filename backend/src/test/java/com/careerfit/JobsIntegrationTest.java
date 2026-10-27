package com.careerfit;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobsIntegrationTest extends IntegrationTestBase {

    private JsonNode getJson(String url, String token) throws Exception {
        var req = get(url);
        if (token != null) req = req.header("Authorization", bearer(token));
        return json(mvc.perform(req).andExpect(status().isOk()).andReturn());
    }

    private void send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, String token, Object body, int expected) throws Exception {
        b = b.header("Authorization", bearer(token));
        if (body != null) b = b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
        mvc.perform(b).andExpect(status().is(expected));
    }

    @Test
    void jobsRequireLoginButPublicListingDoesNot() throws Exception {
        mvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
        JsonNode pub = getJson("/api/public/jobs", null);
        assertEquals(8, pub.get("totalElements").asInt());
        assertTrue(pub.get("content").get(0).get("matchPercent").isNull());
        assertTrue(pub.get("content").get(0).get("sample").asBoolean());
        mvc.perform(get("/api/public/jobs/1")).andExpect(status().isOk());
    }

    @Test
    void filtersWork() throws Exception {
        String t = registerAndGetToken(uniqueEmail());
        assertEquals(2, getJson("/api/jobs?workMode=REMOTE", t).get("totalElements").asInt());
        assertEquals(1, getJson("/api/jobs?q=data%20analyst", t).get("totalElements").asInt());
        assertEquals(1, getJson("/api/jobs?location=pune", t).get("totalElements").asInt());
        assertEquals(2, getJson("/api/jobs?careerPathId=4", t).get("totalElements").asInt());   // Frontend Developer skills
        mvc.perform(get("/api/jobs?workMode=NOPE").header("Authorization", bearer(t))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/jobs/999999").header("Authorization", bearer(t))).andExpect(status().isNotFound());
    }

    @Test
    void matchPercentFollowsTheDocumentedFormula() throws Exception {
        String t = registerAndGetToken(uniqueEmail());
        send(put("/api/profile/skills"), t, Map.of("skillIds", List.of(1, 2, 7)), 200);   // Java Basics, Core Java and OOP, SQL Basics
        JsonNode matches = getJson("/api/jobs/matches", t);
        JsonNode top = matches.get("content").get(0);
        assertEquals(1, top.get("id").asInt());                 // Junior Java Developer
        assertEquals(70.0, top.get("matchPercent").asDouble(), 0.001);   // 0.7 * (3/3) + 0.3 * (0/2)
        JsonNode detail = getJson("/api/jobs/1", t);
        assertEquals(2, detail.get("match").get("missingPreferred").size());
        assertEquals(0, detail.get("match").get("missingRequired").size());
    }

    @Test
    void savedJobsAndApplicationsArePrivatePerUser() throws Exception {
        String a = registerAndGetToken(uniqueEmail());
        String b = registerAndGetToken(uniqueEmail());

        send(post("/api/jobs/1/save"), a, null, 204);
        send(post("/api/jobs/1/save"), a, null, 204);     // idempotent
        assertEquals(1, getJson("/api/jobs/saved", a).size());
        assertEquals(0, getJson("/api/jobs/saved", b).size());
        assertTrue(getJson("/api/jobs/1", a).get("saved").asBoolean());
        assertFalse(getJson("/api/jobs/1", b).get("saved").asBoolean());
        send(delete("/api/jobs/1/save"), b, null, 204);   // B cannot remove A's saved job
        assertEquals(1, getJson("/api/jobs/saved", a).size());
        send(post("/api/jobs/999999/save"), a, null, 404);

        send(put("/api/jobs/1/application"), a, Map.of("status", "APPLIED", "notes", "Applied via portal"), 200);
        send(put("/api/jobs/1/application"), a, Map.of("status", "INTERVIEWING"), 200);   // update, not duplicate
        JsonNode apps = getJson("/api/applications", a);
        assertEquals(1, apps.size());
        assertEquals("INTERVIEWING", apps.get(0).get("status").asText());
        assertEquals(0, getJson("/api/applications", b).size());
        send(put("/api/jobs/1/application"), a, Map.of("status", "BOGUS"), 400);

        send(delete("/api/jobs/1/application"), a, null, 204);
        assertEquals(0, getJson("/api/applications", a).size());
        send(delete("/api/jobs/1/save"), a, null, 204);
        assertEquals(0, getJson("/api/jobs/saved", a).size());
    }
}
