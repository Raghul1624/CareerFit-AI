package com.careerfit;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks of the career-based mock tests against the seeded question bank:
 * the right questions per career path, skill tests that do not mix skills, randomisation,
 * server-side scoring, validation and security.
 */
class CareerMockTestIntegrationTest extends IntegrationTestBase {

    // Career path ids and skill names come from the seed data (V2 / V6).
    private static final long JAVA = 1, PYTHON = 2, FULL_STACK = 3, DATA_ANALYST = 6, AUTOMATION_TESTING = 12;

    private static final Set<String> JAVA_SKILLS = Set.of("Java Basics", "Core Java and OOP", "Advanced Java",
        "SQL Basics", "Spring Boot", "REST APIs");
    private static final Set<String> PYTHON_SKILLS = Set.of("Python Basics", "Python Data Structures",
        "Python Functions and OOP", "SQL Basics");
    private static final Set<String> FULL_STACK_SKILLS = Set.of("HTML and CSS", "JavaScript", "React", "REST APIs",
        "Java Basics", "Spring Boot", "SQL Basics");
    private static final Set<String> DATA_ANALYST_SKILLS = Set.of("SQL Basics", "SQL Joins and Aggregations", "Excel",
        "Statistics", "Data Analysis Fundamentals", "Power BI");
    private static final Set<String> TESTING_SKILLS = Set.of("Manual Testing", "Selenium", "Java Basics", "SQL Basics");

    @Autowired JdbcTemplate jdbc;

    // ------------------------------------------------------------------ helpers

    private JsonNode getJson(String url, String token) throws Exception {
        return json(mvc.perform(get(url).header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
    }

    private JsonNode careerTest(String token, long careerPathId) throws Exception {
        JsonNode list = getJson("/api/mock-tests?mode=CAREER&careerPathId=" + careerPathId, token);
        assertEquals(1, list.size(), "each career path has exactly one career mock test");
        return list.get(0);
    }

    private MvcResult startRaw(String token, long testId) throws Exception {
        return mvc.perform(post("/api/mock-tests/" + testId + "/attempts").header("Authorization", bearer(token)))
            .andExpect(status().isCreated()).andReturn();
    }

    private JsonNode start(String token, long testId) throws Exception {
        return json(startRaw(token, testId));
    }

    private List<Long> questionIds(JsonNode attempt) {
        List<Long> ids = new ArrayList<>();
        attempt.get("questions").forEach(q -> ids.add(q.get("id").asLong()));
        return ids;
    }

    private String skillOf(long questionId) {
        return jdbc.queryForObject("select s.name from questions q join skills s on s.id = q.skill_id where q.id = ?",
            String.class, questionId);
    }

    private Set<String> skillNames(JsonNode attempt) {
        Set<String> names = new TreeSet<>();
        questionIds(attempt).forEach(id -> names.add(skillOf(id)));
        return names;
    }

    private List<Long> optionIds(long questionId, boolean correct) {
        return jdbc.queryForList("select id from question_options where question_id = ? and is_correct = ? order by display_order",
            Long.class, questionId, correct);
    }

    private void saveAnswers(String token, long attemptId, List<Map<String, Object>> answers, int expectedStatus) throws Exception {
        mvc.perform(put("/api/attempts/" + attemptId + "/answers").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(Map.of("answers", answers))))
            .andExpect(status().is(expectedStatus));
    }

    private JsonNode submit(String token, long attemptId) throws Exception {
        return json(mvc.perform(post("/api/attempts/" + attemptId + "/submit").header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andReturn());
    }

    private void assertCareerAttempt(long careerPathId, int expectedCount, Set<String> allowedSkills, int minSkills) throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode test = careerTest(token, careerPathId);
        assertEquals("CAREER", test.get("mode").asText());
        assertEquals(careerPathId, test.get("careerPathId").asLong());

        JsonNode attempt = start(token, test.get("id").asLong());
        assertEquals(expectedCount, attempt.get("questions").size());
        assertEquals("CAREER", attempt.get("mode").asText());
        assertEquals(careerPathId, attempt.get("careerPathId").asLong());

        Set<String> skills = skillNames(attempt);
        assertTrue(allowedSkills.containsAll(skills),
            "question skills " + skills + " must all belong to " + allowedSkills);
        assertTrue(skills.size() >= minSkills,
            "a career test should combine several topics but only used " + skills);
    }

    // ------------------------------------------------------------------ question selection per career

    @Test
    void javaDeveloperTestReturnsOnlyJavaStackQuestions() throws Exception {
        assertCareerAttempt(JAVA, 25, JAVA_SKILLS, 5);
    }

    @Test
    void pythonDeveloperTestReturnsOnlyPythonStackQuestions() throws Exception {
        assertCareerAttempt(PYTHON, 20, PYTHON_SKILLS, 4);
    }

    @Test
    void fullStackDeveloperTestReturnsOnlyFullStackQuestions() throws Exception {
        assertCareerAttempt(FULL_STACK, 25, FULL_STACK_SKILLS, 6);
    }

    @Test
    void dataAnalystTestReturnsOnlyDataAnalystQuestions() throws Exception {
        assertCareerAttempt(DATA_ANALYST, 25, DATA_ANALYST_SKILLS, 5);
    }

    @Test
    void automationTestingTestReturnsOnlyTestingQuestions() throws Exception {
        assertCareerAttempt(AUTOMATION_TESTING, 25, TESTING_SKILLS, 4);
    }

    @Test
    void differentCareerPathsDoNotReceiveTheSameQuestions() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode java = start(token, careerTest(token, JAVA).get("id").asLong());
        JsonNode analyst = start(token, careerTest(token, DATA_ANALYST).get("id").asLong());

        Set<String> javaSkills = skillNames(java);
        Set<String> analystSkills = skillNames(analyst);
        assertTrue(javaSkills.stream().anyMatch(Set.of("Core Java and OOP", "Advanced Java", "Spring Boot", "REST APIs")::contains));
        assertTrue(analystSkills.stream().anyMatch(Set.of("Excel", "Statistics", "Power BI", "Data Analysis Fundamentals")::contains));
        assertTrue(Collections.disjoint(javaSkills, Set.of("Excel", "Statistics", "Power BI", "Data Analysis Fundamentals")));
        assertTrue(Collections.disjoint(analystSkills, Set.of("Core Java and OOP", "Advanced Java", "Spring Boot", "REST APIs")));
        assertNotEquals(new HashSet<>(questionIds(java)), new HashSet<>(questionIds(analyst)));
    }

    @Test
    void everyCareerPathHasAServableCareerTestWithAtLeastFiftyQuestions() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode paths = getJson("/api/career-paths", token);
        assertEquals(20, paths.size());
        for (JsonNode path : paths) {
            JsonNode test = careerTest(token, path.get("id").asLong());
            String name = path.get("name").asText();
            assertTrue(test.get("startable").asBoolean(), name + " test must be startable");
            assertTrue(test.get("availableQuestions").asInt() >= 50, name + " has only " + test.get("availableQuestions").asInt() + " questions");
            assertTrue(test.get("availableQuestions").asInt() >= test.get("questionCount").asInt());
            assertTrue(test.get("topics").size() >= 3, name + " should list the topics it covers");
            assertFalse(test.get("difficulties").isEmpty());
        }
    }

    // ------------------------------------------------------------------ skill tests do not mix skills

    @Test
    void everySkillTestContainsQuestionsOfExactlyItsOwnSkill() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode skillTests = getJson("/api/mock-tests?mode=SKILL", token);
        assertEquals(26, skillTests.size());
        for (JsonNode test : skillTests) {
            assertEquals(1, test.get("skills").size(), test.get("title").asText() + " must cover exactly one skill");
            JsonNode attempt = start(token, test.get("id").asLong());
            assertEquals(test.get("questionCount").asInt(), attempt.get("questions").size());
            assertEquals(Set.of(test.get("skills").get(0).asText()), skillNames(attempt),
                test.get("title").asText() + " must not contain questions of another skill");
            assertEquals("SKILL", attempt.get("mode").asText());
            assertTrue(attempt.get("careerPathId").isNull());
        }
    }

    // ------------------------------------------------------------------ randomisation

    @Test
    void questionsAreNotRepeatedWithinOneAttempt() throws Exception {
        for (long path : new long[]{JAVA, DATA_ANALYST, FULL_STACK}) {
            String token = registerAndGetToken(uniqueEmail());
            List<Long> ids = questionIds(start(token, careerTest(token, path).get("id").asLong()));
            assertEquals(ids.size(), new HashSet<>(ids).size());
        }
    }

    @Test
    void differentUsersAndAttemptsCanReceiveDifferentQuestionSets() throws Exception {
        Set<Set<Long>> sets = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            String token = registerAndGetToken(uniqueEmail());
            sets.add(new HashSet<>(questionIds(start(token, careerTest(token, JAVA).get("id").asLong()))));
        }
        assertTrue(sets.size() > 1, "four independent users should not all receive the identical set");
    }

    @Test
    void aRetakePrefersQuestionsTheUserHasNotSeenYet() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long testId = careerTest(token, JAVA).get("id").asLong();
        JsonNode first = start(token, testId);
        submit(token, first.get("id").asLong());

        JsonNode second = start(token, testId);
        assertNotEquals(first.get("id").asLong(), second.get("id").asLong());
        Set<Long> overlap = new HashSet<>(questionIds(first));
        overlap.retainAll(questionIds(second));
        // The Java pool has 116 questions and a test uses 25, so a full set of unseen questions exists.
        assertTrue(overlap.isEmpty(), "retake repeated " + overlap.size() + " questions although new ones were available");
    }

    // ------------------------------------------------------------------ never short, never padded

    @Test
    void aTestWithTooFewEligibleQuestionsIsRejectedInsteadOfPadded() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        jdbc.update("insert into mock_tests (id, title, description, test_mode, career_path_id, duration_minutes, question_count, "
            + "marks_per_question, negative_marks, passing_percentage, max_attempts, active) "
            + "values (9001, 'Oversized test', 'needs more questions than exist', 'SKILL', null, 10, 500, 1, 0, 40, null, true)");
        jdbc.update("insert into mock_test_skills (mock_test_id, skill_id) values (9001, 12)");   // Qlik Sense has 8 questions
        try {
            MvcResult r = mvc.perform(post("/api/mock-tests/9001/attempts").header("Authorization", bearer(token)))
                .andExpect(status().isConflict()).andReturn();
            assertTrue(r.getResponse().getContentAsString().contains("not available"));

            JsonNode summary = getJson("/api/mock-tests/9001", token);
            assertFalse(summary.get("startable").asBoolean());
            assertEquals(8, summary.get("availableQuestions").asInt());
            assertFalse(summary.get("unavailableReason").asText().isBlank());
        } finally {
            jdbc.update("delete from mock_test_skills where mock_test_id = 9001");
            jdbc.update("delete from mock_tests where id = 9001");
        }
    }

    @Test
    void aCareerTestNeverUsesSkillsThatAreNotPartOfItsCareerPath() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long javaTestId = careerTest(token, JAVA).get("id").asLong();
        jdbc.update("insert into mock_test_skills (mock_test_id, skill_id) values (?, 4)", javaTestId);   // Python Basics: wrong link
        try {
            JsonNode attempt = start(token, javaTestId);
            assertFalse(skillNames(attempt).contains("Python Basics"), "unrelated Python questions must not enter a Java test");
            assertTrue(JAVA_SKILLS.containsAll(skillNames(attempt)));
        } finally {
            jdbc.update("delete from mock_test_skills where mock_test_id = ? and skill_id = 4", javaTestId);
        }
    }

    // ------------------------------------------------------------------ no answer leakage, server-side scoring

    @Test
    void theWholeAttemptPayloadContainsNoAnswerKey() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        long testId = careerTest(token, DATA_ANALYST).get("id").asLong();
        String body = startRaw(token, testId).getResponse().getContentAsString();
        assertFalse(body.contains("\"correct\""));
        assertFalse(body.contains("\"isCorrect\""));
        assertFalse(body.contains("explanation"));

        long attemptId = json(startRaw(token, testId)).get("id").asLong();
        String reloaded = mvc.perform(get("/api/attempts/" + attemptId).header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(reloaded.contains("\"correct\""));
        assertFalse(reloaded.contains("explanation"));
        mvc.perform(get("/api/attempts/" + attemptId + "/result").header("Authorization", bearer(token)))
            .andExpect(status().isConflict());   // answers are only revealed after submission
    }

    @Test
    void allCorrectAnswersScoreOneHundredPercentWithExplanations() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, careerTest(token, JAVA).get("id").asLong());
        List<Map<String, Object>> answers = new ArrayList<>();
        for (long qid : questionIds(attempt)) {
            answers.add(Map.of("questionId", qid, "selectedOptionIds", optionIds(qid, true), "markedForReview", false));
        }
        saveAnswers(token, attempt.get("id").asLong(), answers, 204);
        JsonNode result = submit(token, attempt.get("id").asLong());

        int total = attempt.get("questions").size();
        assertEquals(total, result.get("correctCount").asInt());
        assertEquals(0, result.get("incorrectCount").asInt());
        assertEquals(0, result.get("unansweredCount").asInt());
        assertEquals(100.0, result.get("percentage").asDouble(), 0.001);
        assertTrue(result.get("passed").asBoolean());
        assertEquals("Java Developer", result.get("careerPathName").asText());

        assertEquals(total, result.get("review").size());
        for (JsonNode item : result.get("review")) {
            assertFalse(item.get("explanation").asText().isBlank(), "every question needs an explanation");
            assertTrue(item.get("correct").asBoolean());
        }
        int topicTotal = 0;
        for (JsonNode topic : result.get("topicPerformance")) topicTotal += topic.get("total").asInt();
        assertEquals(total, topicTotal, "topic-wise performance must account for every question");
    }

    @Test
    void wrongAnswersAreMarkedIncorrectAndUnansweredAreCounted() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, careerTest(token, PYTHON).get("id").asLong());
        List<Long> ids = questionIds(attempt);
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {     // answer five questions wrongly, leave the rest blank
            long qid = ids.get(i);
            answers.add(Map.of("questionId", qid, "selectedOptionIds", List.of(optionIds(qid, false).get(0)), "markedForReview", false));
        }
        saveAnswers(token, attempt.get("id").asLong(), answers, 204);
        JsonNode result = submit(token, attempt.get("id").asLong());
        assertEquals(5, result.get("incorrectCount").asInt());
        assertEquals(ids.size() - 5, result.get("unansweredCount").asInt());
        assertEquals(0, result.get("correctCount").asInt());
        assertEquals(0.0, result.get("score").asDouble(), 0.001);
    }

    // ------------------------------------------------------------------ invalid input

    @Test
    void invalidAnswerSubmissionsAreRejected() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode attempt = start(token, careerTest(token, JAVA).get("id").asLong());
        long attemptId = attempt.get("id").asLong();
        List<Long> ids = questionIds(attempt);
        long q0 = ids.get(0), q1 = ids.get(1);

        // a question that is not part of this attempt
        saveAnswers(token, attemptId, List.of(Map.of("questionId", 999_999L, "selectedOptionIds", List.of(1L), "markedForReview", false)), 400);
        // an option id that does not exist
        saveAnswers(token, attemptId, List.of(Map.of("questionId", q0, "selectedOptionIds", List.of(987_654_321L), "markedForReview", false)), 400);
        // an option that belongs to a different question
        saveAnswers(token, attemptId, List.of(Map.of("questionId", q0, "selectedOptionIds", List.of(optionIds(q1, true).get(0)), "markedForReview", false)), 400);
        // two options for a single-choice question
        List<Long> all = jdbc.queryForList("select id from question_options where question_id = ?", Long.class, q0);
        String type = jdbc.queryForObject("select question_type from questions where id = ?", String.class, q0);
        if (!"MULTIPLE_CHOICE".equals(type)) {
            saveAnswers(token, attemptId, List.of(Map.of("questionId", q0, "selectedOptionIds", List.of(all.get(0), all.get(1)), "markedForReview", false)), 400);
        }
        // too many option ids in one answer (bean validation)
        List<Long> eleven = new ArrayList<>();
        for (long i = 1; i <= 11; i++) eleven.add(i);
        saveAnswers(token, attemptId, List.of(Map.of("questionId", q0, "selectedOptionIds", eleven, "markedForReview", false)), 400);
        // missing body
        mvc.perform(put("/api/attempts/" + attemptId + "/answers").header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());

        // unknown test and unknown attempt
        mvc.perform(post("/api/mock-tests/987654/attempts").header("Authorization", bearer(token))).andExpect(status().isNotFound());
        mvc.perform(post("/api/attempts/987654/submit").header("Authorization", bearer(token))).andExpect(status().isNotFound());
        mvc.perform(get("/api/mock-tests/987654").header("Authorization", bearer(token))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ security

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/mock-tests")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mock-tests/6")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/mock-tests/6/attempts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/attempts/1")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/attempts/1/answers").contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[]}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/attempts/1/submit")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/attempts/1/result")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/attempts/history")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/career-paths/1")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ test history and career context

    @Test
    void resultAndHistoryCarryTheCareerPathAndTestType() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode career = start(token, careerTest(token, JAVA).get("id").asLong());
        JsonNode result = submit(token, career.get("id").asLong());
        assertEquals("CAREER", result.get("mode").asText());
        assertEquals(JAVA, result.get("careerPathId").asLong());

        JsonNode skillAttempt = start(token, 3L);        // Python Basics Skill Test
        submit(token, skillAttempt.get("id").asLong());

        JsonNode history = getJson("/api/attempts/history", token);
        assertEquals(2, history.get("totalElements").asInt());
        boolean sawCareer = false, sawSkill = false;
        for (JsonNode row : history.get("content")) {
            if ("CAREER".equals(row.get("mode").asText())) {
                sawCareer = true;
                assertEquals("Java Developer", row.get("careerPathName").asText());
            } else {
                sawSkill = true;
                assertTrue(row.get("careerPathName").isNull());
            }
        }
        assertTrue(sawCareer && sawSkill);
    }

    // ------------------------------------------------------------------ filters and career page

    @Test
    void mockTestListSupportsTypeCareerAndSkillFilters() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        assertEquals(20, getJson("/api/mock-tests?mode=CAREER", token).size());
        assertEquals(26, getJson("/api/mock-tests?mode=SKILL", token).size());

        JsonNode forAnalyst = getJson("/api/mock-tests?careerPathId=" + DATA_ANALYST, token);
        assertEquals(1, forAnalyst.size());
        assertEquals("Data Analyst Mock Test", forAnalyst.get(0).get("title").asText());

        Set<String> titles = new HashSet<>();
        getJson("/api/mock-tests?skillId=7", token).forEach(t -> titles.add(t.get("title").asText()));   // SQL Basics
        assertTrue(titles.contains("SQL Basics Skill Test"));
        assertTrue(titles.contains("Java Developer Mock Test"));
        assertTrue(titles.contains("Data Analyst Mock Test"));
        assertFalse(titles.contains("Excel Skill Test"));
        assertFalse(titles.contains("Verbal Ability Mock Test"));
    }

    @Test
    void careerPathDetailShowsOverviewSkillsTopicsRoadmapAndItsOwnMockTest() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        JsonNode detail = getJson("/api/career-paths/" + DATA_ANALYST, token);
        assertFalse(detail.get("description").asText().isBlank());
        assertTrue(detail.get("skills").size() >= 6);
        assertEquals("CAREER", detail.get("careerMockTest").get("mode").asText());
        assertEquals(DATA_ANALYST, detail.get("careerMockTest").get("careerPathId").asLong());
        assertEquals(detail.get("careerMockTest").get("id").asLong(), detail.get("recommendedTests").get(0).get("id").asLong(),
            "the career test is listed first");
        assertFalse(detail.get("learningTopics").isEmpty());
        assertFalse(detail.get("learningTopics").get(0).get("topics").isEmpty());
        assertFalse(detail.get("roadmap").isNull());
        assertFalse(detail.get("suggestedLearningOrder").isEmpty());
        // other careers' mock tests are not recommended on this page
        for (JsonNode t : detail.get("recommendedTests")) {
            if ("CAREER".equals(t.get("mode").asText())) assertEquals(DATA_ANALYST, t.get("careerPathId").asLong());
        }
        assertFalse(getJson("/api/career-paths/" + AUTOMATION_TESTING, token).get("roadmap").isNull());
        mvc.perform(get("/api/career-paths/987654").header("Authorization", bearer(token))).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ question bank integrity

    @Test
    void questionBankIsConsistent() {
        Integer total = jdbc.queryForObject("select count(*) from questions where active = true", Integer.class);
        assertTrue(total >= 550, "expected the full bank, found " + total);

        List<String> texts = jdbc.queryForList("select question_text from questions", String.class);
        assertEquals(texts.size(), new HashSet<>(texts).size(), "question texts must be unique");

        Integer badKey = jdbc.queryForObject("""
            select count(*) from (
              select q.id from questions q join question_options o on o.question_id = q.id
              where q.question_type in ('SINGLE_CHOICE', 'TRUE_FALSE')
              group by q.id having sum(case when o.is_correct = true then 1 else 0 end) <> 1) x
            """, Integer.class);
        assertEquals(0, badKey, "single-choice questions need exactly one correct option");

        Integer noExplanation = jdbc.queryForObject(
            "select count(*) from questions where explanation is null or explanation = '' or topic is null or topic = ''", Integer.class);
        assertEquals(0, noExplanation);

        // The correct option must not always be listed first (the original seed data did exactly that).
        Integer single = jdbc.queryForObject("select count(*) from questions where question_type = 'SINGLE_CHOICE'", Integer.class);
        Integer firstCorrect = jdbc.queryForObject("""
            select count(*) from question_options o join questions q on q.id = o.question_id
            where q.question_type = 'SINGLE_CHOICE' and o.is_correct = true and o.display_order = 1
            """, Integer.class);
        assertTrue(firstCorrect < single * 0.4, "correct answers must be spread over the positions");
    }
}
