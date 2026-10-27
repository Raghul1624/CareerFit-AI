package com.careerfit;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void registerThenLoginAndReadProfile() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email);

        String login = om.writeValueAsString(Map.of("email", email, "password", "Passw0rdTest"));
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
            .andExpect(status().isOk()).andReturn();
        assertFalse(r.getResponse().getContentAsString().toLowerCase().contains("password"));

        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
            .andExpect(status().isOk());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email);
        String body = om.writeValueAsString(Map.of("fullName", "Other", "email", email, "password", "Passw0rdTest"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        String body = om.writeValueAsString(Map.of("fullName", "Weak", "email", uniqueEmail(), "password", "short"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email);
        String login = om.writeValueAsString(Map.of("email", email, "password", "WrongPass123"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/career-paths")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer not-a-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void ordinaryUserCannotAccessAdminApi() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        mvc.perform(get("/api/admin/summary").header("Authorization", bearer(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    void careerPathsAreReturnedForAuthenticatedUser() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        MvcResult r = mvc.perform(get("/api/career-paths").header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andReturn();
        assertEquals(20, json(r).size());   // 20 seeded sample career paths
    }
}
