package com.careerfit.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Pure, deterministic, rule-based resume analysis. No I/O except reading the alias rules at startup. */
@Component
public class ResumeAnalyzer {

    public record Result(List<String> extractedSkills, List<String> missingSkills, Double coverage,
                         List<String> suggestions, int wordCount, int pageCount) {}

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s-]{8,}\\d");
    private static final Pattern PERCENT = Pattern.compile("\\d+\\s*%");

    private final Map<String, List<Pattern>> rules = new LinkedHashMap<>();

    public ResumeAnalyzer() {
        try (InputStream in = getClass().getResourceAsStream("/resume/skill-aliases.txt")) {
            if (in == null) throw new IllegalStateException("resume/skill-aliases.txt is missing");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains(":")) continue;
                String skill = line.substring(0, line.indexOf(':')).trim();
                List<Pattern> patterns = new ArrayList<>();
                for (String alias : line.substring(line.indexOf(':') + 1).split(",")) {
                    if (!alias.isBlank()) patterns.add(pattern(alias));
                }
                rules.put(skill, patterns);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read resume skill rules", e);
        }
    }

    private static Pattern pattern(String alias) {
        return Pattern.compile("(?<![a-z0-9+#])" + Pattern.quote(alias.trim().toLowerCase()) + "(?![a-z0-9+#])");
    }

    public Result analyze(String rawText, int pageCount, Collection<String> knownSkills, Collection<String> careerSkills) {
        String text = rawText == null ? "" : rawText.toLowerCase().replaceAll("\\s+", " ").trim();
        int words = text.isEmpty() ? 0 : text.split(" ").length;

        List<String> detected = new ArrayList<>();
        for (String skill : new TreeSet<>(knownSkills)) {
            List<Pattern> patterns = rules.getOrDefault(skill, List.of(pattern(skill)));
            if (patterns.stream().anyMatch(p -> p.matcher(text).find())) detected.add(skill);
        }
        List<String> missing = careerSkills.stream().filter(s -> !detected.contains(s)).sorted().toList();
        Double coverage = null;
        if (!careerSkills.isEmpty()) {
            long hit = careerSkills.stream().filter(detected::contains).count();
            coverage = Math.round(hit * 1000.0 / careerSkills.size()) / 10.0;
        }

        List<String> tips = new ArrayList<>();
        if (words < 50) {
            tips.add("Very little text could be read from this PDF. If it is a scanned image, export a text-based PDF instead.");
        } else {
            if (words < 150) tips.add("The resume is short (" + words + " words). Add detail about projects, internships and coursework.");
            if (words > 1000) tips.add("The resume is long (" + words + " words). Freshers usually do best with one page, two at most.");
            if (pageCount > 2) tips.add("The resume has " + pageCount + " pages. Consider trimming it to one or two pages.");
            if (!EMAIL.matcher(text).find()) tips.add("No email address was detected. Make sure your contact details are in plain text.");
            if (!PHONE.matcher(text).find()) tips.add("No phone number was detected.");
            if (!text.contains("github.com")) tips.add("Add your GitHub link so reviewers can see your code.");
            if (!text.contains("linkedin.com")) tips.add("Add your LinkedIn profile link.");
            addSectionTip(tips, text, "\\bprojects?\\b", "Add a Projects section describing what you built, the technologies used and your role.");
            addSectionTip(tips, text, "\\beducation\\b|\\bacademic", "Add an Education section with your degree, institution and year.");
            addSectionTip(tips, text, "\\bskills\\b", "Add a Skills section that lists your technical skills clearly.");
            addSectionTip(tips, text, "\\bexperience\\b|\\binternship", "Add an Experience or Internship section, even for short internships or training.");
            addSectionTip(tips, text, "\\bcertifications?\\b|\\bcertificates?\\b", "If you have certifications, list them in a Certifications section.");
            if (!PERCENT.matcher(text).find()) {
                tips.add("Where truthful, quantify results (percentages, counts, time saved) instead of only listing duties.");
            }
        }
        if (!missing.isEmpty()) {
            tips.add("Skills expected for this career but not found in the text: " + String.join(", ", missing)
                + ". Add them only if you genuinely have the skill, ideally backed by a project or coursework.");
        }
        return new Result(detected, missing, coverage, tips, words, pageCount);
    }

    private static void addSectionTip(List<String> tips, String text, String regex, String tip) {
        if (!Pattern.compile(regex).matcher(text).find()) tips.add(tip);
    }
}
