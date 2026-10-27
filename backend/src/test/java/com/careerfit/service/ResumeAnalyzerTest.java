package com.careerfit.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumeAnalyzerTest {

    private final ResumeAnalyzer analyzer = new ResumeAnalyzer();
    private final List<String> known = List.of("Java Basics", "JavaScript", "React", "SQL Basics", "Spring Boot", "Excel");

    @Test
    void javaDoesNotMatchJavascript() {
        var r = analyzer.analyze("Skills: JavaScript, React.js and HTML", 1, known, List.of());
        assertTrue(r.extractedSkills().contains("JavaScript"));
        assertTrue(r.extractedSkills().contains("React"));
        assertFalse(r.extractedSkills().contains("Java Basics"));
    }

    @Test
    void coverageIsMatchedRequiredSkillsOverRequiredSkills() {
        var r = analyzer.analyze("Built a project with Java, MySQL and Spring Boot.", 1, known,
            List.of("Java Basics", "SQL Basics", "Spring Boot", "Excel"));
        assertEquals(75.0, r.coverage());
        assertEquals(List.of("Excel"), r.missingSkills());
    }

    @Test
    void coverageIsNullWithoutCareer() {
        assertNull(analyzer.analyze("java developer", 1, known, List.of()).coverage());
    }

    @Test
    void nearEmptyTextProducesScannedPdfAdvice() {
        var r = analyzer.analyze("", 1, known, List.of("Java Basics"));
        assertEquals(0, r.wordCount());
        assertTrue(r.suggestions().get(0).contains("scanned"));
    }

    @Test
    void suggestsMissingSectionsAndLinks() {
        String text = "Raghul. Java developer. " + "I like building software and learning new tools every day. ".repeat(20);
        var r = analyzer.analyze(text, 1, known, List.of());
        String all = String.join("|", r.suggestions());
        assertTrue(all.contains("Projects section"));
        assertTrue(all.contains("GitHub"));
        assertTrue(all.contains("email"));
    }
}
