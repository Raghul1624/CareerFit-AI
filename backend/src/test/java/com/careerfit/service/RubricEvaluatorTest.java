package com.careerfit.service;

import com.careerfit.service.RubricEvaluator.ConceptSpec;
import com.careerfit.service.RubricEvaluator.Evaluation;
import com.careerfit.service.RubricEvaluator.Mode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests of the transparent interview rubric (pure logic, no Spring context). */
class RubricEvaluatorTest {

    private static final List<ConceptSpec> JVM_RUBRIC = List.of(
        new ConceptSpec("JVM executes bytecode", List.of("jvm", "bytecode", "virtual machine"), 2, true),
        new ConceptSpec("JRE supplies the runtime libraries", List.of("jre", "runtime environment", "libraries"), 1, true),
        new ConceptSpec("JDK adds development tools", List.of("jdk", "compiler", "javac"), 2, true),
        new ConceptSpec("Platform independence", List.of("platform independen", "write once", "portable"), 1, false));

    private static final String GOOD_ANSWER =
        "The JVM is the virtual machine that executes Java bytecode, which makes Java platform independent. "
            + "The JRE contains the JVM together with the standard libraries needed to run a program. "
            + "The JDK is the full development kit: it contains the JRE plus tools such as the javac compiler, "
            + "so you need the JDK to write and compile code but only the JRE to run it. For example, a customer who only runs an application needs just the JRE.";

    private static final List<ConceptSpec> HR_RUBRIC = List.of(
        new ConceptSpec("Your actions", List.of("i decided", "i organised", "i talked", "i built"), 3, true),
        new ConceptSpec("Result and lesson", List.of("result", "learned", "improved"), 2, true),
        new ConceptSpec("The situation", List.of("team", "project", "deadline"), 2, true));

    private static int dim(Evaluation e, String name) {
        return e.dimensions().stream().filter(d -> d.name().equals(name)).findFirst().orElseThrow().score();
    }

    @Test
    void aCompleteAnswerScoresHighAndListsEveryConceptAsCovered() {
        Evaluation e = RubricEvaluator.evaluate(GOOD_ANSWER, JVM_RUBRIC, Mode.TECHNICAL);
        assertTrue(e.overall() >= 80, "overall was " + e.overall());
        assertEquals(4, e.matchedConcepts().size());
        assertTrue(e.missedConcepts().isEmpty());
        assertEquals(100, dim(e, "Correctness"));
    }

    @Test
    void aMissingEssentialConceptLowersCorrectnessAndIsNamedInTheFeedback() {
        String answer = "The JVM executes bytecode and the JRE provides the runtime environment with libraries. "
            + "Together they let a program run on different systems, because the bytecode is portable and the machine takes care of the details.";
        Evaluation e = RubricEvaluator.evaluate(answer, JVM_RUBRIC, Mode.TECHNICAL);
        assertTrue(dim(e, "Correctness") < 80);
        assertTrue(e.missedConcepts().contains("JDK adds development tools"));
        assertTrue(e.improvements().stream().anyMatch(s -> s.contains("JDK adds development tools")));
    }

    @Test
    void betterAnswersScoreHigherThanWeakerOnes() {
        int good = RubricEvaluator.evaluate(GOOD_ANSWER, JVM_RUBRIC, Mode.TECHNICAL).overall();
        int partial = RubricEvaluator.evaluate("The JVM runs bytecode and the JDK has a compiler, which is used by developers when they write programs.", JVM_RUBRIC, Mode.TECHNICAL).overall();
        int weak = RubricEvaluator.evaluate("It is a java thing for programs.", JVM_RUBRIC, Mode.TECHNICAL).overall();
        assertTrue(good > partial, good + " should beat " + partial);
        assertTrue(partial > weak, partial + " should beat " + weak);
    }

    @Test
    void blankAndNoAnswerResponsesScoreAlmostNothingButNeverFail() {
        assertTrue(RubricEvaluator.evaluate("", JVM_RUBRIC, Mode.TECHNICAL).overall() <= 5);
        assertTrue(RubricEvaluator.evaluate(null, JVM_RUBRIC, Mode.TECHNICAL).overall() <= 5);
        Evaluation idk = RubricEvaluator.evaluate("I don't know", JVM_RUBRIC, Mode.TECHNICAL);
        assertTrue(idk.overall() <= 5);
        assertTrue(idk.improvements().stream().anyMatch(s -> s.toLowerCase().contains("don't know")));
    }

    @Test
    void aBareKeywordListDoesNotScoreLikeARealExplanation() {
        String stuffed = "jvm bytecode virtual machine jre runtime environment libraries jdk compiler javac platform independent portable";
        int stuffedScore = RubricEvaluator.evaluate(stuffed, JVM_RUBRIC, Mode.TECHNICAL).overall();
        int real = RubricEvaluator.evaluate(GOOD_ANSWER, JVM_RUBRIC, Mode.TECHNICAL).overall();
        assertTrue(stuffedScore < real - 15, "keyword stuffing scored " + stuffedScore + " versus " + real);
    }

    @Test
    void repeatingTheSamePhraseIsPenalisedInClarity() {
        String repeated = "jvm bytecode ".repeat(40);
        Evaluation e = RubricEvaluator.evaluate(repeated, JVM_RUBRIC, Mode.TECHNICAL);
        assertTrue(dim(e, "Clarity") < 60, "clarity was " + dim(e, "Clarity"));
    }

    @Test
    void theDimensionWeightsAddUpToOneHundredAndTheOverallScoreIsTheirWeightedSum() {
        for (Mode mode : Mode.values()) {
            Evaluation e = RubricEvaluator.evaluate(GOOD_ANSWER, JVM_RUBRIC, mode);
            assertEquals(4, e.dimensions().size());
            assertEquals(100, e.dimensions().stream().mapToInt(RubricEvaluator.Dimension::weightPercent).sum());
            double sum = e.dimensions().stream().mapToDouble(d -> d.score() * d.weightPercent() / 100.0).sum();
            assertEquals(Math.round(sum), e.overall());
        }
    }

    @Test
    void sameAnswerAlwaysGivesTheSameResult() {
        Evaluation a = RubricEvaluator.evaluate(GOOD_ANSWER, JVM_RUBRIC, Mode.TECHNICAL);
        Evaluation b = RubricEvaluator.evaluate(GOOD_ANSWER, JVM_RUBRIC, Mode.TECHNICAL);
        assertEquals(a, b);
    }

    @Test
    void keywordMatchingIsCaseInsensitiveAndStemBased() {
        Evaluation e = RubricEvaluator.evaluate("JAVA IS PLATFORM INDEPENDENT. The JVM RUNS BYTECODE.", JVM_RUBRIC, Mode.TECHNICAL);
        assertTrue(e.matchedConcepts().contains("Platform independence"));
        assertTrue(e.matchedConcepts().contains("JVM executes bytecode"));
    }

    @Test
    void aStructuredStarAnswerBeatsAVagueHrAnswer() {
        String star = "During my final year project our team of four had a tight deadline. My task was to build the login module. "
            + "I decided to split the work into small parts, I talked to every teammate each day and I built the core feature first. "
            + "As a result we delivered on time, the page load improved by 30 percent and I learned how to plan work with Git and Jira. For example, we used daily check-ins.";
        String vague = "I am a hard worker and I like teamwork. I always do my best.";
        Evaluation good = RubricEvaluator.evaluate(star, HR_RUBRIC, Mode.HR);
        Evaluation bad = RubricEvaluator.evaluate(vague, HR_RUBRIC, Mode.HR);
        assertTrue(good.overall() >= 70, "STAR answer scored " + good.overall());
        assertTrue(bad.overall() < 40, "vague answer scored " + bad.overall());
        assertTrue(dim(good, "Specificity") > dim(bad, "Specificity"));
        assertTrue(dim(good, "Structure") > dim(bad, "Structure"));
    }

    @Test
    void hrFeedbackAsksForNumbersAndResultsWhenTheyAreMissing() {
        Evaluation e = RubricEvaluator.evaluate("I worked on a project with my team and we talked a lot about the plan every day at college.", HR_RUBRIC, Mode.HR);
        assertTrue(e.improvements().stream().anyMatch(s -> s.contains("number")));
        assertTrue(e.improvements().stream().anyMatch(s -> s.toLowerCase().contains("result")));
    }

    @Test
    void anEmptyRubricDoesNotCrash() {
        Evaluation e = RubricEvaluator.evaluate("Some answer with a few words in it that is long enough to count here.", List.of(), Mode.TECHNICAL);
        assertTrue(e.overall() >= 0 && e.overall() <= 100);
    }
}
