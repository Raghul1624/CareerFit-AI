package com.careerfit.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transparent, rule-based evaluation of a written interview answer. No AI service is involved, so it always works,
 * gives the same result for the same answer and every score can be explained.
 *
 * <p><b>Technical interview</b> (weights in %): Correctness 35 (required rubric concepts covered), Completeness 25
 * (all concepts covered, adjusted for very short answers), Clarity 20 (length, sentences, repetition, connectors) and
 * Technical understanding 20 (precise terminology and depth cues such as examples, reasons, trade-offs).
 *
 * <p><b>HR interview</b>: Relevance 30 (rubric concepts covered), Structure 25 (organised, STAR-style story),
 * Specificity 25 (numbers, named technologies, concrete examples) and Communication 20 (the same clarity score).
 *
 * <p>Limits, stated honestly in the UI: it checks coverage of ideas and the shape of the answer, it cannot judge whether
 * every claim is true, and a model answer is shown after submission for comparison.
 */
public final class RubricEvaluator {

    public enum Mode { TECHNICAL, HR }

    public record ConceptSpec(String name, List<String> keywords, int weight, boolean required) {}

    public record Dimension(String name, int score, int weightPercent, String comment) {}

    public record Evaluation(int overall, List<Dimension> dimensions, List<String> strengths, List<String> improvements,
                             List<String> matchedConcepts, List<String> missedConcepts, int wordCount) {}

    private static final Pattern WORD = Pattern.compile("[a-z0-9']+");
    private static final Pattern NUMBER = Pattern.compile("\\b\\d+(?:[.,]\\d+)?\\s?(?:%|percent|ms|seconds|users|days|weeks|months|years|x)?");
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?]+(?:\\s|$)");
    private static final Pattern PROPER_NOUN = Pattern.compile("(?<![.!?]\\s)(?<!^)\\b[A-Z][A-Za-z0-9+#.]{2,}\\b");
    private static final List<String> NO_ANSWER = List.of("i don't know", "i dont know", "no idea", "idk", "not sure", "can't answer", "cannot answer");
    private static final List<String> CONNECTORS = List.of("because", "therefore", "however", "for example", "for instance", "first", "then",
        "finally", "also", "so that", "which means", "as a result", "in addition", "while", "whereas", "instead", "next");
    private static final List<String> EXAMPLE_CUES = List.of("for example", "for instance", "e.g.", "such as", "in my project", "when i ", "once,", "in one project");
    private static final List<String> REASON_CUES = List.of("because", "therefore", "so that", "which means", "this is why", "as a result", "since ");
    private static final List<String> TRADEOFF_CUES = List.of("however", "trade-off", "tradeoff", "but ", "drawback", "limitation", "downside", "whereas", "unlike", "instead of", "on the other hand");
    private static final List<String> PERF_CUES = List.of("o(", "complexity", "performance", "scal", "memory", "faster", "slower", "efficien", "latency", "thread-safe");
    private static final List<String> SITUATION_CUES = List.of("when ", "situation", "project", "team", "during", "at my", "last year", "semester", "internship", "once ");
    private static final List<String> TASK_CUES = List.of("my task", "my role", "i was responsible", "responsible for", "needed to", "goal was", "had to", "my job", "objective");
    private static final Pattern ACTION = Pattern.compile("\\bi (?:[a-z]+ed|built|led|made|took|ran|wrote|chose|set|began|started|worked|decided|talked|asked|gave|found|kept|held|did|learned|learnt|taught|fixed|solved|proposed|created)\\b");
    private static final List<String> RESULT_CUES = List.of("result", "outcome", "as a result", "finally", "achieved", "improved", "reduced", "increased", "learned", "learnt", "so we", "successful", "delivered", "completed");

    private RubricEvaluator() {
    }

    public static Evaluation evaluate(String answer, List<ConceptSpec> concepts, Mode mode) {
        String raw = answer == null ? "" : answer.strip();
        String text = raw.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        int words = countWords(text);

        // concept coverage
        List<String> matched = new ArrayList<>();
        List<String> missed = new ArrayList<>();
        List<String> requiredMissed = new ArrayList<>();
        double allWeight = 0, allHit = 0, reqWeight = 0, reqHit = 0;
        Set<String> distinctKeywordsHit = new LinkedHashSet<>();
        Set<String> distinctKeywords = new LinkedHashSet<>();
        for (ConceptSpec c : concepts) {
            boolean hit = false;
            for (String k : c.keywords()) {
                String kw = k.toLowerCase(Locale.ROOT).strip();
                if (kw.isEmpty()) continue;
                distinctKeywords.add(kw);
                if (text.contains(kw)) {
                    hit = true;
                    distinctKeywordsHit.add(kw);
                }
            }
            allWeight += c.weight();
            if (c.required()) reqWeight += c.weight();
            if (hit) {
                allHit += c.weight();
                if (c.required()) reqHit += c.weight();
                matched.add(c.name());
            } else {
                missed.add(c.name());
                if (c.required()) requiredMissed.add(c.name());
            }
        }
        double coverageAll = allWeight == 0 ? 0 : allHit / allWeight;
        double coverageRequired = reqWeight == 0 ? coverageAll : reqHit / reqWeight;

        boolean noAnswer = words < 12 && NO_ANSWER.stream().anyMatch(text::contains);
        int clarity = clarity(raw, text, words, distinctKeywordsHit.size());

        List<Dimension> dims = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        if (mode == Mode.TECHNICAL) {
            int correctness = pct(coverageRequired);
            int completeness = pct(coverageAll * Math.min(1.0, 0.5 + words / 80.0));
            int understanding = understanding(text, distinctKeywordsHit.size(), distinctKeywords.size());
            dims.add(new Dimension("Correctness", correctness, 35, "Share of the essential ideas your answer covers (" + (concepts.size() - missed.size()) + " of " + concepts.size() + " ideas found)."));
            dims.add(new Dimension("Completeness", completeness, 25, "Coverage of all expected ideas, reduced when the answer is very short (" + words + " words)."));
            dims.add(new Dimension("Clarity", clarity, 20, "Length, sentence structure, linking words and absence of repetition."));
            dims.add(new Dimension("Technical understanding", understanding, 20, "Precise terminology plus examples, reasons, trade-offs or performance points."));
        } else {
            int relevance = pct(coverageAll);
            int structure = structure(raw, text);
            int specificity = specificity(raw, text);
            dims.add(new Dimension("Relevance", relevance, 30, "How many of the points this question looks for you addressed (" + (concepts.size() - missed.size()) + " of " + concepts.size() + ")."));
            dims.add(new Dimension("Structure", structure, 25, "Whether the answer is organised: situation, your role, your actions and the result."));
            dims.add(new Dimension("Specificity", specificity, 25, "Concrete detail: numbers, named tools or projects and real examples."));
            dims.add(new Dimension("Communication", clarity, 20, "Length, sentence structure, linking words and absence of repetition."));
        }
        double total = 0;
        for (Dimension d : dims) total += d.score() * d.weightPercent() / 100.0;
        int overall = (int) Math.round(total);
        if (noAnswer) overall = Math.min(overall, 5);
        if (words < 8) overall = Math.min(overall, 10);
        dims = capIfNoAnswer(dims, noAnswer || words < 8);

        for (String m : matched) strengths.add("You covered: " + m + ".");
        if (clarity >= 75) strengths.add("Clear and well organised: good length and linking words.");
        if (containsAny(text, EXAMPLE_CUES)) strengths.add("You used a concrete example.");
        if (strengths.isEmpty()) strengths.add("You attempted the question; build on it with the points below.");

        if (noAnswer) improvements.add("Do not answer 'I don't know': explain what you do know and how you would find out the rest.");
        for (String r : requiredMissed) improvements.add("Explain this essential point: " + r + ".");
        for (String r : missed) if (!requiredMissed.contains(r)) improvements.add("Also mention: " + r + ".");
        if (words < 30) improvements.add("Write a fuller answer: aim for at least 40 to 60 words.");
        if (words > 400) improvements.add("Shorten the answer: the key points get lost in more than 400 words.");
        if (!containsAny(text, EXAMPLE_CUES)) improvements.add("Add a short concrete example.");
        if (mode == Mode.TECHNICAL && !containsAny(text, REASON_CUES)) improvements.add("Explain why, not only what (use because, therefore).");
        if (mode == Mode.HR && !NUMBER.matcher(text).find()) improvements.add("Add a number or measurable result (for example a percentage or a time saved).");
        if (mode == Mode.HR && !containsAny(text, RESULT_CUES)) improvements.add("End with the result and what you learned.");
        if (improvements.size() > 7) improvements = new ArrayList<>(improvements.subList(0, 7));

        return new Evaluation(overall, dims, strengths, improvements, matched, missed, words);
    }

    // ------------------------------------------------------------------------------------------ dimensions

    static int clarity(String raw, String text, int words, int distinctKeywordsHit) {
        int score;
        if (words < 15) score = 20;
        else if (words < 30) score = 55;
        else if (words < 60) score = 85;
        else if (words <= 250) score = 100;
        else if (words <= 400) score = 85;
        else score = 70;
        int sentences = Math.max(1, countMatches(SENTENCE_END, raw));
        if (sentences < 2 && words > 25) score -= 15;                      // one long run-on
        double avg = words / (double) sentences;
        if (avg > 35) score -= 15;
        else if (avg < 5 && words > 15) score -= 10;
        if (words >= 20) {
            Set<String> unique = new HashSet<>(wordsOf(text));
            double ratio = unique.size() / (double) words;
            if (ratio < 0.4) score -= 25;
            else if (ratio < 0.5) score -= 12;
        }
        if (words < 60 && distinctKeywordsHit / (double) Math.max(1, words) > 0.35) score -= 25;   // keyword list, not an explanation
        int connectors = 0;
        for (String c : CONNECTORS) if (text.contains(c)) connectors++;
        if (connectors < 2 && words >= 30) score -= 10;
        return Math.max(0, Math.min(100, score));
    }

    static int understanding(String text, int distinctHit, int distinctTotal) {
        double terminology = Math.min(1.0, distinctHit / (double) Math.max(1, Math.min(8, distinctTotal)));
        int cues = 0;
        if (containsAny(text, EXAMPLE_CUES)) cues++;
        if (containsAny(text, REASON_CUES)) cues++;
        if (containsAny(text, TRADEOFF_CUES)) cues++;
        if (containsAny(text, PERF_CUES)) cues++;
        return (int) Math.round(terminology * 60 + cues * 10);
    }

    static int structure(String raw, String text) {
        int score = 0;
        int sentences = countMatches(SENTENCE_END, raw);
        if (sentences >= 3) score += 25;
        else if (sentences == 2) score += 12;
        int connectors = 0;
        for (String c : CONNECTORS) if (text.contains(c)) connectors++;
        if (connectors >= 3) score += 25;
        else if (connectors >= 1) score += 12;
        int star = 0;
        if (containsAny(text, SITUATION_CUES)) star++;
        if (containsAny(text, TASK_CUES)) star++;
        if (ACTION.matcher(text).find()) star++;
        if (containsAny(text, RESULT_CUES)) star++;
        score += star * 12;                                                // up to 48
        return Math.min(100, score + (star == 4 ? 2 : 0));
    }

    static int specificity(String raw, String text) {
        int score = 0;
        if (NUMBER.matcher(text).find()) score += 30;
        Matcher m = PROPER_NOUN.matcher(raw);
        int names = 0;
        while (m.find()) names++;
        if (names >= 2) score += 30;
        else if (names == 1) score += 15;
        if (containsAny(text, EXAMPLE_CUES)) score += 40;
        else if (ACTION.matcher(text).find()) score += 20;
        return Math.min(100, score);
    }

    // ------------------------------------------------------------------------------------------ helpers

    private static List<Dimension> capIfNoAnswer(List<Dimension> dims, boolean cap) {
        if (!cap) return dims;
        List<Dimension> out = new ArrayList<>();
        for (Dimension d : dims) out.add(new Dimension(d.name(), Math.min(d.score(), 10), d.weightPercent(), d.comment()));
        return out;
    }

    private static int pct(double v) {
        return (int) Math.round(Math.max(0, Math.min(1, v)) * 100);
    }

    private static boolean containsAny(String text, List<String> cues) {
        for (String c : cues) if (text.contains(c)) return true;
        return false;
    }

    private static int countMatches(Pattern p, String s) {
        Matcher m = p.matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private static int countWords(String text) {
        return wordsOf(text).size();
    }

    private static List<String> wordsOf(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = WORD.matcher(text);
        while (m.find()) out.add(m.group());
        return out;
    }
}
