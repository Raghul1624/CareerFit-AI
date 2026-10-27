package com.careerfit.service;

import com.careerfit.entity.Difficulty;
import com.careerfit.repository.QuestionRef;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests of the selection algorithm: no Spring context, seeded randomness. */
class QuestionSelectorTest {

    private static long nextId;

    /** Builds a pool: for every skill, `topicsPerSkill` topics with one question of each difficulty, `repeat` times. */
    private static List<QuestionRef> pool(int skills, int topicsPerSkill, int repeat) {
        List<QuestionRef> list = new ArrayList<>();
        nextId = 1;
        for (long s = 1; s <= skills; s++) {
            for (int t = 1; t <= topicsPerSkill; t++) {
                for (int r = 0; r < repeat; r++) {
                    for (Difficulty d : Difficulty.values()) {
                        list.add(new QuestionRef(nextId++, s, "S" + s + "-T" + t, d));
                    }
                }
            }
        }
        return list;
    }

    private static Map<Long, Long> perSkill(List<Long> ids, List<QuestionRef> pool) {
        Map<Long, QuestionRef> byId = pool.stream().collect(Collectors.toMap(QuestionRef::id, q -> q));
        return ids.stream().collect(Collectors.groupingBy(id -> byId.get(id).skillId(), Collectors.counting()));
    }

    @Test
    void returnsRequestedCountWithoutDuplicates() {
        List<QuestionRef> pool = pool(6, 4, 2);
        for (int seed = 0; seed < 50; seed++) {
            List<Long> ids = QuestionSelector.select(pool, 25, Set.of(), new Random(seed));
            assertEquals(25, ids.size());
            assertEquals(25, new HashSet<>(ids).size(), "no question may appear twice in one attempt");
        }
    }

    @Test
    void onlyUsesQuestionsFromThePool() {
        List<QuestionRef> pool = pool(3, 3, 1);
        Set<Long> allowed = pool.stream().map(QuestionRef::id).collect(Collectors.toSet());
        List<Long> ids = QuestionSelector.select(pool, 10, Set.of(), new Random(1));
        assertTrue(allowed.containsAll(ids));
    }

    @Test
    void duplicatePoolEntriesAreCountedOnce() {
        List<QuestionRef> pool = new ArrayList<>(pool(1, 2, 1));   // 6 questions
        pool.addAll(new ArrayList<>(pool));                         // every question listed twice
        List<Long> ids = QuestionSelector.select(pool, 6, Set.of(), new Random(3));
        assertEquals(6, new HashSet<>(ids).size());
        assertThrows(IllegalArgumentException.class, () -> QuestionSelector.select(pool, 7, Set.of(), new Random(3)));
    }

    @Test
    void splitsTheTestFairlyAcrossSkills() {
        List<QuestionRef> pool = pool(6, 4, 2);          // 6 skills, 24 questions each
        for (int seed = 0; seed < 50; seed++) {
            Map<Long, Long> counts = perSkill(QuestionSelector.select(pool, 25, Set.of(), new Random(seed)), pool);
            assertEquals(6, counts.size(), "every skill must be represented");
            counts.values().forEach(c -> assertTrue(c == 4 || c == 5, "25 questions over 6 skills means 4 or 5 each, got " + c));
        }
    }

    @Test
    void aSmallSkillHandsItsUnusedShareToTheOthers() {
        List<QuestionRef> pool = new ArrayList<>(pool(2, 3, 2));            // skills 1 and 2: 18 questions each
        pool.add(new QuestionRef(9_001L, 3L, "tiny", Difficulty.BEGINNER));  // skill 3 has only one question
        Map<Long, Long> counts = perSkill(QuestionSelector.select(pool, 12, Set.of(), new Random(5)), pool);
        assertEquals(1L, counts.get(3L).longValue());
        assertEquals(12L, counts.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void spreadsQuestionsAcrossTopicsInsideASkill() {
        List<QuestionRef> pool = pool(1, 6, 3);          // one skill, 6 topics, 9 questions per topic
        for (int seed = 0; seed < 30; seed++) {
            Map<Long, QuestionRef> byId = pool.stream().collect(Collectors.toMap(QuestionRef::id, q -> q));
            Map<String, Long> perTopic = QuestionSelector.select(pool, 12, Set.of(), new Random(seed)).stream()
                .collect(Collectors.groupingBy(id -> byId.get(id).topic(), Collectors.counting()));
            assertEquals(6, perTopic.size(), "all six topics should appear in a 12-question test");
            perTopic.values().forEach(c -> assertEquals(2L, c.longValue(), "12 questions over 6 topics means exactly 2 each"));
        }
    }

    @Test
    void difficultyFollowsTheTargetMixWhenTheBankAllowsIt() {
        List<QuestionRef> pool = pool(4, 5, 4);          // plenty of every difficulty
        int[] totals = new int[3];
        int attempts = 100;
        Map<Long, QuestionRef> byId = pool.stream().collect(Collectors.toMap(QuestionRef::id, q -> q));
        for (int seed = 0; seed < attempts; seed++) {
            for (Long id : QuestionSelector.select(pool, 20, Set.of(), new Random(seed))) {
                totals[byId.get(id).difficulty().ordinal()]++;
            }
        }
        double all = attempts * 20.0;
        assertEquals(0.30, totals[0] / all, 0.03);
        assertEquals(0.45, totals[1] / all, 0.03);
        assertEquals(0.25, totals[2] / all, 0.03);
    }

    @Test
    void difficultyTargetsAlwaysAddUpToTheCount() {
        for (int count = 1; count <= 60; count++) {
            int sum = QuestionSelector.difficultyTargets(count).values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(count, sum);
        }
    }

    @Test
    void usesWhateverDifficultyExistsWhenTheMixCannotBeMet() {
        // Only BEGINNER questions exist; the test must still be filled, from the questions that do exist.
        List<QuestionRef> pool = new ArrayList<>();
        for (long i = 1; i <= 20; i++) pool.add(new QuestionRef(i, 1L, "T" + (i % 3), Difficulty.BEGINNER));
        assertEquals(15, QuestionSelector.select(pool, 15, Set.of(), new Random(2)).size());
    }

    @Test
    void differentAttemptsGetDifferentQuestionSets() {
        List<QuestionRef> pool = pool(6, 4, 2);
        Set<List<Long>> distinct = new HashSet<>();
        Random rnd = new Random(11);
        for (int i = 0; i < 30; i++) {
            List<Long> ids = new ArrayList<>(QuestionSelector.select(pool, 25, Set.of(), rnd));
            Collections.sort(ids);
            distinct.add(ids);
        }
        assertTrue(distinct.size() >= 25, "30 attempts should almost always produce different sets, got " + distinct.size());
    }

    @Test
    void aRetakeAvoidsQuestionsAlreadySeenWhenEnoughNewOnesExist() {
        List<QuestionRef> pool = pool(6, 4, 2);          // 144 questions
        Random rnd = new Random(21);
        Set<Long> seen = new HashSet<>(QuestionSelector.select(pool, 25, Set.of(), rnd));
        for (int i = 0; i < 50; i++) {
            List<Long> second = QuestionSelector.select(pool, 25, seen, rnd);
            assertTrue(second.stream().noneMatch(seen::contains), "retake repeated a question although new ones were available");
        }
    }

    @Test
    void seenQuestionsAreUsedOnlyWhenNothingNewIsLeft() {
        List<QuestionRef> pool = pool(1, 2, 2);          // 12 questions
        Set<Long> seen = pool.stream().limit(10).map(QuestionRef::id).collect(Collectors.toSet());
        List<Long> ids = QuestionSelector.select(pool, 12, seen, new Random(4));
        assertEquals(12, ids.size());                    // everything must be used, including the seen ones
        List<Long> two = QuestionSelector.select(pool, 2, seen, new Random(4));
        assertTrue(two.stream().noneMatch(seen::contains), "the two unseen questions must be preferred");
    }

    @Test
    void refusesToPadAShortPool() {
        List<QuestionRef> pool = pool(1, 1, 1);          // 3 questions
        assertThrows(IllegalArgumentException.class, () -> QuestionSelector.select(pool, 4, Set.of(), new Random(1)));
        assertTrue(QuestionSelector.select(pool, 0, Set.of(), new Random(1)).isEmpty());
    }

    @Test
    void questionsWithoutATopicStillWork() {
        List<QuestionRef> pool = new ArrayList<>();
        for (long i = 1; i <= 8; i++) pool.add(new QuestionRef(i, 1L, i % 2 == 0 ? null : "  ", Difficulty.INTERMEDIATE));
        assertEquals(5, QuestionSelector.select(pool, 5, null, new Random(8)).size());
    }
}
