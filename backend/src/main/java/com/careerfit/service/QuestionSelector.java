package com.careerfit.service;

import com.careerfit.entity.Difficulty;
import com.careerfit.repository.QuestionRef;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Chooses which questions go into one test attempt. Pure logic (no Spring, no database), so it is
 * easy to unit test with a seeded random generator.
 *
 * <ol>
 *   <li>The question count is split fairly across the skills in the pool (a Java Developer test with six
 *       skills gets roughly the same number of questions from each); a skill with too few questions
 *       gives its unused share to the others.</li>
 *   <li>Inside a skill the topics are visited round-robin in random order, so one topic cannot dominate.</li>
 *   <li>Difficulty is steered towards {@link #TARGET_MIX} across the WHOLE test. It is a preference: when
 *       a topic has no question of the wanted difficulty another difficulty is used.</li>
 *   <li>Questions in {@code avoid} (ones the user already saw in earlier attempts of the same test) are
 *       only used when nothing unseen is left, so repeated attempts differ whenever the bank allows it.</li>
 *   <li>The final order is shuffled. A question is never returned twice.</li>
 * </ol>
 */
public final class QuestionSelector {

    /** Preferred share of each difficulty level in a test. */
    public static final Map<Difficulty, Double> TARGET_MIX = Collections.unmodifiableMap(new EnumMap<>(Map.of(
        Difficulty.BEGINNER, 0.30, Difficulty.INTERMEDIATE, 0.45, Difficulty.ADVANCED, 0.25)));

    private static final String NO_TOPIC = "General";

    private QuestionSelector() {
    }

    /**
     * @param pool  every eligible question (already filtered to the right skills)
     * @param count how many questions to select; must not exceed the number of distinct questions in the pool
     * @param avoid question ids to use only as a last resort (may be null)
     * @return the selected question ids in the order they should appear in the test
     * @throws IllegalArgumentException when the pool is smaller than {@code count}
     */
    public static List<Long> select(Collection<QuestionRef> pool, int count, Set<Long> avoid, RandomGenerator rnd) {
        if (count <= 0) {
            return List.of();
        }
        Map<Long, QuestionRef> unique = new LinkedHashMap<>();
        for (QuestionRef r : pool) {
            unique.putIfAbsent(r.id(), r);
        }
        if (unique.size() < count) {
            throw new IllegalArgumentException("Pool has " + unique.size() + " questions but " + count + " were requested");
        }
        Set<Long> seen = avoid == null ? Set.of() : avoid;

        // Shuffle, then move questions the user has not seen yet to the front (stable partition).
        List<QuestionRef> shuffled = new ArrayList<>(unique.values());
        Collections.shuffle(shuffled, rnd);
        List<QuestionRef> ordered = new ArrayList<>(shuffled.size());
        for (QuestionRef r : shuffled) {
            if (!seen.contains(r.id())) ordered.add(r);
        }
        for (QuestionRef r : shuffled) {
            if (seen.contains(r.id())) ordered.add(r);
        }

        // skill -> topic -> difficulty -> queue (unseen first)
        Map<Long, Map<String, EnumMap<Difficulty, ArrayDeque<QuestionRef>>>> tree = new LinkedHashMap<>();
        Map<Long, Integer> capacity = new LinkedHashMap<>();
        for (QuestionRef r : ordered) {
            tree.computeIfAbsent(r.skillId(), k -> new LinkedHashMap<>())
                .computeIfAbsent(topicOf(r), k -> new EnumMap<>(Difficulty.class))
                .computeIfAbsent(r.difficulty(), k -> new ArrayDeque<>())
                .addLast(r);
            capacity.merge(r.skillId(), 1, Integer::sum);
        }

        List<Long> skills = new ArrayList<>(tree.keySet());
        Collections.shuffle(skills, rnd);
        Map<Long, Integer> quota = allocateQuota(skills, capacity, count);
        EnumMap<Difficulty, Integer> want = difficultyTargets(count);
        EnumMap<Difficulty, Integer> picked = new EnumMap<>(Difficulty.class);

        List<QuestionRef> result = new ArrayList<>(count);
        for (Long skill : skills) {
            int need = quota.get(skill);
            Map<String, EnumMap<Difficulty, ArrayDeque<QuestionRef>>> topics = tree.get(skill);
            List<String> topicOrder = new ArrayList<>(topics.keySet());
            Collections.shuffle(topicOrder, rnd);
            int taken = 0;
            while (taken < need) {
                // Pass 1 takes only questions the user has not seen; pass 2 (fallback) may repeat one.
                boolean progressed = false;
                for (boolean unseenOnly : new boolean[]{true, false}) {
                    for (String topic : topicOrder) {
                        if (taken >= need) break;
                        QuestionRef r = pollMostNeeded(topics.get(topic), want, picked, rnd, seen, unseenOnly);
                        if (r != null) {
                            result.add(r);
                            picked.merge(r.difficulty(), 1, Integer::sum);
                            taken++;
                            progressed = true;
                        }
                    }
                    if (progressed) break;
                }
                if (!progressed) break;   // cannot happen while quota <= capacity, kept as a safety net
            }
        }
        Collections.shuffle(result, rnd);
        return result.stream().map(QuestionRef::id).toList();
    }

    /** Equal share per skill; a skill that cannot supply its share hands the remainder to the others. */
    static Map<Long, Integer> allocateQuota(List<Long> skills, Map<Long, Integer> capacity, int count) {
        int n = skills.size();
        int base = count / n;
        int remainder = count % n;
        Map<Long, Integer> quota = new LinkedHashMap<>();
        int assigned = 0;
        for (int i = 0; i < n; i++) {
            Long skill = skills.get(i);
            int share = Math.min(capacity.get(skill), base + (i < remainder ? 1 : 0));
            quota.put(skill, share);
            assigned += share;
        }
        while (assigned < count) {
            boolean progressed = false;
            for (Long skill : skills) {
                if (assigned < count && quota.get(skill) < capacity.get(skill)) {
                    quota.merge(skill, 1, Integer::sum);
                    assigned++;
                    progressed = true;
                }
            }
            if (!progressed) break;
        }
        return quota;
    }

    /** Largest-remainder split of {@code count} according to {@link #TARGET_MIX}. */
    static EnumMap<Difficulty, Integer> difficultyTargets(int count) {
        EnumMap<Difficulty, Integer> result = new EnumMap<>(Difficulty.class);
        EnumMap<Difficulty, Double> fraction = new EnumMap<>(Difficulty.class);
        int total = 0;
        for (Difficulty d : Difficulty.values()) {
            double exact = TARGET_MIX.getOrDefault(d, 0.0) * count;
            int whole = (int) Math.floor(exact);
            result.put(d, whole);
            fraction.put(d, exact - whole);
            total += whole;
        }
        List<Difficulty> byFraction = new ArrayList<>(List.of(Difficulty.values()));
        byFraction.sort((a, b) -> Double.compare(fraction.get(b), fraction.get(a)));
        for (int i = 0; total < count; i = (i + 1) % byFraction.size()) {
            result.merge(byFraction.get(i), 1, Integer::sum);
            total++;
        }
        return result;
    }

    /**
     * Takes a question of the difficulty that is currently furthest below its target, if the topic has one.
     * With {@code unseenOnly} a question the user already saw is never returned; because every queue keeps
     * unseen questions in front, "head is unseen" is the same as "queue still has an unseen question".
     * Avoiding repeats therefore takes priority over hitting the exact difficulty mix.
     */
    private static QuestionRef pollMostNeeded(EnumMap<Difficulty, ArrayDeque<QuestionRef>> byDifficulty,
                                              EnumMap<Difficulty, Integer> want,
                                              EnumMap<Difficulty, Integer> picked, RandomGenerator rnd,
                                              Set<Long> seen, boolean unseenOnly) {
        List<Difficulty> order = new ArrayList<>(List.of(Difficulty.values()));
        Collections.shuffle(order, rnd);                       // random tie-break
        order.sort((a, b) -> Integer.compare(
            want.getOrDefault(b, 0) - picked.getOrDefault(b, 0),
            want.getOrDefault(a, 0) - picked.getOrDefault(a, 0)));
        for (Difficulty d : order) {
            ArrayDeque<QuestionRef> queue = byDifficulty.get(d);
            if (queue == null || queue.isEmpty()) continue;
            if (unseenOnly && seen.contains(queue.peekFirst().id())) continue;
            return queue.pollFirst();
        }
        return null;
    }

    private static String topicOf(QuestionRef r) {
        return r.topic() == null || r.topic().isBlank() ? NO_TOPIC : r.topic();
    }
}
