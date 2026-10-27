package com.careerfit.service;

import com.careerfit.dto.CommonDtos.PageResponse;
import com.careerfit.dto.InterviewDtos.*;
import com.careerfit.entity.*;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.*;
import com.careerfit.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Interview attempts: start, save answers, submit, results and history.
 * Every attempt is loaded with the owner's user id, so one user can never read or change another user's attempt.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewAttemptService {

    private static final double MCQ_POINTS = 1.0;
    private static final double OPEN_POINTS = 100.0;          // coding problems and written answers are scored 0..100
    private static final int MAX_TEXT_CHARS = 5000;
    private static final int GRACE_SECONDS = 30;
    private static final double GOOD_ANSWER_THRESHOLD = 60;

    private final InterviewRoleRepository roles;
    private final InterviewRoundConfigRepository configs;
    private final InterviewQuestionRepository questions;
    private final InterviewAttemptRepository attempts;
    private final InterviewTestCaseRepository testCases;
    private final InterviewRubricRepository rubrics;
    private final InterviewCodingProblemRepository codingProblems;
    private final CodeSubmissionRepository submissions;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------ start

    @Transactional
    public AttemptView start(StartRequest req) {
        User user = currentUser.get();
        InterviewRole role = roles.findByCodeAndActiveTrue(req.roleCode().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview role not found"));

        Optional<InterviewAttempt> open = attempts.findFirstByUserIdAndRoleIdAndRoundAndLevelAndStatusOrderByStartedAtDesc(
            user.getId(), role.getId(), req.round(), req.level(), AttemptStatus.IN_PROGRESS);
        if (open.isPresent()) {
            InterviewAttempt existing = open.get();
            if (Instant.now().isBefore(existing.getExpiresAt())) {
                return toView(existing, Instant.now());          // resume instead of creating a duplicate
            }
            finalizeAttempt(existing, true);
        }

        InterviewRoundConfig cfg = configs.findByRoundAndLevel(req.round(), req.level())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This round is not configured for that level"));
        List<Object[]> poolRows = questions.findPool(role.getId(), req.round(), req.level());
        if (poolRows.size() < cfg.getQuestionCount()) {
            throw new ApiException(HttpStatus.CONFLICT, "This assessment is not available yet: it needs " + cfg.getQuestionCount()
                + " questions but only " + poolRows.size() + " are available");
        }
        List<QuestionRef> pool = new ArrayList<>();
        for (Object[] r : poolRows) {
            pool.add(new QuestionRef((Long) r[0], 0L, (String) r[1], Difficulty.INTERMEDIATE));   // one pool: topics are balanced, questions never repeat
        }
        Set<Long> seen = new HashSet<>(attempts.findSeenQuestionIds(user.getId(), role.getId(), req.round(), req.level()));
        List<Long> chosen = QuestionSelector.select(pool, cfg.getQuestionCount(), seen, ThreadLocalRandom.current());
        Map<Long, InterviewQuestion> loaded = questions.findAllWithOptionsByIdIn(chosen).stream()
            .collect(Collectors.toMap(InterviewQuestion::getId, q -> q));

        Instant now = Instant.now();
        InterviewAttempt attempt = new InterviewAttempt();
        attempt.setUser(user);
        attempt.setRole(role);
        attempt.setRound(req.round());
        attempt.setLevel(req.level());
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(now);
        attempt.setExpiresAt(now.plus(Duration.ofMinutes(cfg.getDurationMinutes())));
        attempt.setDurationMinutes(cfg.getDurationMinutes());
        attempt.setPassingPercentage(cfg.getPassingPercentage());
        attempt.setTotalQuestions(chosen.size());
        double max = 0;
        int seq = 1;
        for (Long id : chosen) {
            InterviewQuestion q = loaded.get(id);
            if (q == null) throw new ApiException(HttpStatus.CONFLICT, "A selected question is no longer available; please try again");
            InterviewAttemptItem item = new InterviewAttemptItem();
            item.setAttempt(attempt);
            item.setQuestion(q);
            item.setSeqNo(seq++);
            item.setMaxScore(req.round().isMultipleChoice() ? MCQ_POINTS : OPEN_POINTS);
            max += item.getMaxScore();
            attempt.getItems().add(item);
        }
        attempt.setMaxScore(max);
        attempts.save(attempt);
        return toView(attempt, now);
    }

    // ------------------------------------------------------------------ read and save

    @Transactional
    public AttemptView get(Long id) {
        InterviewAttempt a = owned(id);
        autoSubmitIfExpired(a);
        return toView(a, Instant.now());
    }

    /**
     * Saves answers. Everything is validated BEFORE anything is changed, so a rejected request never leaves a partial save.
     * {@code noRollbackFor}: when the time limit has passed the attempt is auto-submitted and a 409 is returned; that
     * submission must be committed, not rolled back with the exception.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void saveAnswers(Long id, SaveAnswersRequest req) {
        InterviewAttempt a = requireOpen(owned(id));
        Map<Long, InterviewAttemptItem> byQuestion = a.getItems().stream()
            .collect(Collectors.toMap(i -> i.getQuestion().getId(), i -> i));
        Map<Long, InterviewQuestion> qs = loadQuestions(byQuestion.keySet());

        record Prepared(InterviewAttemptItem item, InterviewQuestionType type, String optionIds, String text, String language, boolean marked) {}
        List<Prepared> prepared = new ArrayList<>();
        for (AnswerInput in : req.answers()) {
            InterviewAttemptItem item = byQuestion.get(in.questionId());
            if (item == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Question " + in.questionId() + " is not part of this attempt");
            InterviewQuestion q = qs.get(in.questionId());
            String optionIds = null, text = null, language = null;
            switch (q.getType()) {
                case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                    Set<Long> valid = q.getOptions().stream().map(InterviewOption::getId).collect(Collectors.toSet());
                    List<Long> chosen = in.selectedOptionIds() == null ? List.of() : in.selectedOptionIds().stream().distinct().toList();
                    if (!valid.containsAll(chosen)) throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid option for question " + q.getId());
                    if (q.getType() == InterviewQuestionType.SINGLE_CHOICE && chosen.size() > 1) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Question " + q.getId() + " accepts only one answer");
                    }
                    optionIds = chosen.isEmpty() ? null : chosen.stream().map(String::valueOf).collect(Collectors.joining(","));
                }
                case TEXT -> {
                    String t = in.answerText() == null ? "" : in.answerText();
                    if (t.length() > MAX_TEXT_CHARS) throw new ApiException(HttpStatus.BAD_REQUEST, "Answers are limited to " + MAX_TEXT_CHARS + " characters");
                    text = t.isBlank() ? null : t;
                }
                case CODING -> {
                    text = in.answerText() == null || in.answerText().isBlank() ? null : in.answerText();
                    if (in.language() != null && !in.language().isBlank()) language = parseLanguage(in.language()).name();
                }
            }
            prepared.add(new Prepared(item, q.getType(), optionIds, text, language, Boolean.TRUE.equals(in.markedForReview())));
        }
        for (Prepared p : prepared) {
            switch (p.type()) {
                case SINGLE_CHOICE, MULTIPLE_CHOICE -> p.item().setSelectedOptionIds(p.optionIds());
                case TEXT -> p.item().setAnswerText(p.text());
                case CODING -> {
                    p.item().setAnswerText(p.text());
                    if (p.language() != null) p.item().setLanguage(p.language());
                }
            }
            p.item().setMarkedForReview(p.marked());
        }
    }

    // ------------------------------------------------------------------ submit and results

    @Transactional(noRollbackFor = ApiException.class)
    public ResultView submit(Long id) {
        InterviewAttempt a = owned(id);
        if (!a.isFinished()) finalizeAttempt(a, false);
        return buildResult(a);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public ResultView result(Long id) {
        InterviewAttempt a = owned(id);
        autoSubmitIfExpired(a);
        if (!a.isFinished()) {
            throw new ApiException(HttpStatus.CONFLICT, "Results and correct answers are available after you submit the attempt");
        }
        return buildResult(a);
    }

    @Transactional(readOnly = true)
    public PageResponse<HistoryItem> history(int page, int size, InterviewRound round, String roleCode) {
        Long userId = currentUser.get().getId();
        Specification<InterviewAttempt> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("user").get("id"), userId));
            if (round != null) p.add(cb.equal(root.get("round"), round));
            if (roleCode != null && !roleCode.isBlank()) p.add(cb.equal(root.get("role").get("code"), roleCode.toUpperCase(Locale.ROOT)));
            return cb.and(p.toArray(new Predicate[0]));
        };
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "startedAt"));
        return PageResponse.from(attempts.findAll(spec, pr).map(this::toHistoryItem));
    }

    /** Safety net: finalizes attempts whose time ran out and that were never submitted. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sweepExpired() {
        for (InterviewAttempt a : attempts.findByStatusAndExpiresAtBefore(AttemptStatus.IN_PROGRESS, Instant.now().minusSeconds(GRACE_SECONDS))) {
            try {
                finalizeAttempt(a, true);
            } catch (RuntimeException e) {
                log.warn("Could not auto-submit interview attempt {}: {}", a.getId(), e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ shared helpers (also used by the coding service)

    /** Loads an attempt of the CURRENT user or fails with 404. */
    InterviewAttempt owned(Long id) {
        return attempts.findByIdAndUserId(id, currentUser.get().getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
    }

    /** The attempt must still be open and inside its time limit; an expired attempt is submitted automatically. */
    InterviewAttempt requireOpen(InterviewAttempt a) {
        if (a.isFinished()) throw new ApiException(HttpStatus.CONFLICT, "This attempt has already been submitted");
        if (Instant.now().isAfter(a.getExpiresAt().plusSeconds(GRACE_SECONDS))) {
            finalizeAttempt(a, true);
            throw new ApiException(HttpStatus.CONFLICT, "Time is up: your attempt was submitted automatically");
        }
        return a;
    }

    /** True when the time limit (plus a small grace period for network delay) has passed. */
    boolean isPastDeadline(InterviewAttempt a) {
        return Instant.now().isAfter(a.getExpiresAt().plusSeconds(GRACE_SECONDS));
    }

    private void autoSubmitIfExpired(InterviewAttempt a) {
        if (!a.isFinished() && Instant.now().isAfter(a.getExpiresAt().plusSeconds(GRACE_SECONDS))) finalizeAttempt(a, true);
    }

    static CodeLanguage parseLanguage(String value) {
        try {
            return CodeLanguage.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported language: " + value);
        }
    }

    HistoryItem toHistoryItem(InterviewAttempt a) {
        return new HistoryItem(a.getId(), a.getRole().getCode(), a.getRole().getName(), a.getRound(), a.getRound().label(), a.getLevel(),
            a.getStatus(), a.getScore(), a.getMaxScore(), a.getPercentage(), a.getPassed(), a.getStartedAt(), a.getSubmittedAt(), a.getTotalQuestions());
    }

    private Map<Long, InterviewQuestion> loadQuestions(Collection<Long> ids) {
        return questions.findAllWithOptionsByIdIn(ids).stream().collect(Collectors.toMap(InterviewQuestion::getId, q -> q));
    }

    // ------------------------------------------------------------------ view of a running attempt (no answer key)

    private AttemptView toView(InterviewAttempt a, Instant now) {
        List<InterviewAttemptItem> items = a.getItems();
        Set<Long> ids = items.stream().map(i -> i.getQuestion().getId()).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, InterviewQuestion> qs = loadQuestions(ids);
        boolean coding = a.getRound().isCoding();
        Map<Long, InterviewCodingProblem> problems = coding
            ? codingProblems.findAllById(ids).stream().collect(Collectors.toMap(InterviewCodingProblem::getQuestionId, p -> p)) : Map.of();
        Map<Long, List<InterviewTestCase>> tests = coding
            ? testCases.findByQuestionIdInOrderByQuestionIdAscDisplayOrderAsc(ids).stream().collect(Collectors.groupingBy(InterviewTestCase::getQuestionId))
            : Map.of();

        List<QuestionView> views = new ArrayList<>();
        for (InterviewAttemptItem item : items) {
            InterviewQuestion q = qs.get(item.getQuestion().getId());
            List<OptionView> options = null;
            CodingView codingView = null;
            if (q.getType() == InterviewQuestionType.SINGLE_CHOICE || q.getType() == InterviewQuestionType.MULTIPLE_CHOICE) {
                options = q.getOptions().stream().map(o -> new OptionView(o.getId(), o.getText())).toList();
            } else if (q.getType() == InterviewQuestionType.CODING) {
                InterviewCodingProblem p = problems.get(q.getId());
                List<InterviewTestCase> all = tests.getOrDefault(q.getId(), List.of());
                codingView = new CodingView(p.getConstraints(), p.getInputFormat(), p.getOutputFormat(),
                    all.stream().filter(t -> !t.isHidden()).map(t -> new SampleCase(t.getInput(), t.getExpectedOutput())).toList(),
                    (int) all.stream().filter(InterviewTestCase::isHidden).count(), p.getTimeLimitSeconds(),
                    List.of(CodeLanguage.values()), CodingTemplates.starters(),
                    submissions.findBestScore(item.getId()), (int) submissions.countByAttemptItemId(item.getId()));
            }
            views.add(new QuestionView(q.getId(), item.getSeqNo(), q.getType(), q.getTopic(), q.getTitle(), q.getText(), q.getGuidance(),
                options, codingView, new SavedAnswer(parseIds(item.getSelectedOptionIds()).stream().toList(), item.getAnswerText(),
                item.getLanguage(), item.isMarkedForReview())));
        }
        return new AttemptView(a.getId(), a.getRole().getCode(), a.getRole().getName(), a.getRound(), a.getRound().label(), a.getLevel(),
            a.getStatus(), a.getStartedAt(), a.getExpiresAt(), now, a.getDurationMinutes(), a.getTotalQuestions(), a.getPassingPercentage(), views);
    }

    // ------------------------------------------------------------------ scoring

    private record Scored(InterviewAttemptItem item, InterviewQuestion question, RubricEvaluator.Evaluation evaluation) {}

    void finalizeAttempt(InterviewAttempt a, boolean auto) {
        List<InterviewAttemptItem> items = a.getItems();
        Set<Long> ids = items.stream().map(i -> i.getQuestion().getId()).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, InterviewQuestion> qs = loadQuestions(ids);
        Map<Long, List<InterviewRubricConcept>> rubric = a.getRound().isText()
            ? rubrics.findByQuestionIdInOrderByQuestionIdAscDisplayOrderAsc(ids).stream().collect(Collectors.groupingBy(InterviewRubricConcept::getQuestionId))
            : Map.of();
        RubricEvaluator.Mode mode = a.getRound() == InterviewRound.HR_INTERVIEW ? RubricEvaluator.Mode.HR : RubricEvaluator.Mode.TECHNICAL;

        double total = 0;
        List<Scored> scored = new ArrayList<>();
        for (InterviewAttemptItem item : items) {
            InterviewQuestion q = qs.get(item.getQuestion().getId());
            RubricEvaluator.Evaluation evaluation = null;
            double awarded;
            if (a.getRound().isMultipleChoice()) {
                Set<Long> chosen = parseIds(item.getSelectedOptionIds());
                Set<Long> right = q.getOptions().stream().filter(InterviewOption::isCorrect).map(InterviewOption::getId).collect(Collectors.toSet());
                boolean ok = !chosen.isEmpty() && chosen.equals(right);
                item.setCorrect(ok);
                awarded = ok ? MCQ_POINTS : 0;
            } else if (a.getRound().isCoding()) {
                Double best = submissions.findBestScore(item.getId());
                awarded = best == null ? 0 : best;
                item.setCorrect(awarded >= 100.0);
            } else {
                List<RubricEvaluator.ConceptSpec> specs = rubric.getOrDefault(q.getId(), List.of()).stream()
                    .map(c -> new RubricEvaluator.ConceptSpec(c.getName(), Arrays.stream(c.getKeywords().split(",")).map(String::strip).toList(),
                        c.getWeight(), c.isRequired())).toList();
                evaluation = RubricEvaluator.evaluate(item.getAnswerText(), specs, mode);
                awarded = evaluation.overall();
                item.setCorrect(awarded >= GOOD_ANSWER_THRESHOLD);
                item.setEvaluationJson(toJson(toView(evaluation)));
            }
            item.setAwardedScore(awarded);
            total += awarded;
            scored.add(new Scored(item, q, evaluation));
        }
        a.setScore(round2(total));
        a.setPercentage(a.getMaxScore() == 0 ? 0.0 : round2(total / a.getMaxScore() * 100));
        a.setPassed(a.getPercentage() >= a.getPassingPercentage());
        a.setStatus(auto ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);
        a.setSubmittedAt(Instant.now());
        a.setSummaryFeedback(summarise(a, scored));
    }

    private String summarise(InterviewAttempt a, List<Scored> scored) {
        StringBuilder sb = new StringBuilder();
        int n = scored.size();
        long right = scored.stream().filter(s -> Boolean.TRUE.equals(s.item().getCorrect())).count();
        String verdict = Boolean.TRUE.equals(a.getPassed()) ? "You passed this round." : "You did not reach the pass mark of "
            + (int) a.getPassingPercentage() + "% yet.";
        if (a.getRound().isMultipleChoice()) {
            sb.append("You answered ").append(right).append(" of ").append(n).append(" questions correctly (").append(a.getPercentage()).append("%). ").append(verdict);
            Map<String, int[]> byTopic = new LinkedHashMap<>();
            for (Scored s : scored) {
                int[] c = byTopic.computeIfAbsent(topicOf(s.question()), k -> new int[2]);
                c[0]++;
                if (Boolean.TRUE.equals(s.item().getCorrect())) c[1]++;
            }
            List<String> weak = byTopic.entrySet().stream().filter(e -> e.getValue()[1] < e.getValue()[0]).map(Map.Entry::getKey).limit(4).toList();
            if (!weak.isEmpty()) sb.append(" Topics to revise: ").append(String.join(", ", weak)).append(".");
        } else if (a.getRound().isCoding()) {
            long attempted = scored.stream().filter(s -> s.item().getAwardedScore() != null && s.item().getAwardedScore() > 0).count();
            sb.append("You fully solved ").append(right).append(" of ").append(n).append(" problems and your average test-case score was ")
                .append(a.getPercentage()).append("%. ").append(verdict);
            if (right < n) sb.append(" Re-read the failing cases in the review below: edge cases and large inputs are the usual cause.");
            if (attempted < n) sb.append(" Some problems were not submitted, and only submitted code is scored.");
        } else {
            sb.append("Your average answer score was ").append(a.getPercentage()).append("%. ").append(verdict);
            Map<String, double[]> dims = new LinkedHashMap<>();
            Map<String, Integer> missed = new LinkedHashMap<>();
            for (Scored s : scored) {
                for (RubricEvaluator.Dimension d : s.evaluation().dimensions()) {
                    double[] c = dims.computeIfAbsent(d.name(), k -> new double[2]);
                    c[0] += d.score();
                    c[1]++;
                }
                for (String m : s.evaluation().missedConcepts()) missed.merge(m, 1, Integer::sum);
            }
            Optional<Map.Entry<String, double[]>> best = dims.entrySet().stream().max(Comparator.comparingDouble(e -> e.getValue()[0] / e.getValue()[1]));
            Optional<Map.Entry<String, double[]>> worst = dims.entrySet().stream().min(Comparator.comparingDouble(e -> e.getValue()[0] / e.getValue()[1]));
            best.ifPresent(e -> sb.append(" Strongest area: ").append(e.getKey()).append("."));
            worst.ifPresent(e -> sb.append(" Weakest area: ").append(e.getKey()).append("."));
            if (a.getRound() == InterviewRound.HR_INTERVIEW) sb.append(" Tip: tell each story with the STAR structure and finish with a measurable result.");
            else sb.append(" Tip: explain the idea, give a short example and say why it matters.");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ result view

    private ResultView buildResult(InterviewAttempt a) {
        Set<Long> ids = a.getItems().stream().map(i -> i.getQuestion().getId()).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, InterviewQuestion> qs = loadQuestions(ids);
        List<ResultItem> items = new ArrayList<>();
        Map<String, int[]> topics = new LinkedHashMap<>();
        int correctCount = 0;
        for (InterviewAttemptItem item : a.getItems()) {
            InterviewQuestion q = qs.get(item.getQuestion().getId());
            List<ResultOption> options = null;
            EvaluationView evaluation = null;
            CodingResult coding = null;
            String yourAnswer = item.getAnswerText();
            if (a.getRound().isMultipleChoice()) {
                Set<Long> chosen = parseIds(item.getSelectedOptionIds());
                options = q.getOptions().stream().map(o -> new ResultOption(o.getId(), o.getText(), o.isCorrect(), chosen.contains(o.getId()))).toList();
            } else if (a.getRound().isCoding()) {
                Optional<CodeSubmission> last = submissions.findFirstByAttemptItemIdOrderByCreatedAtDescIdDesc(item.getId());
                coding = new CodingResult(submissions.findBestScore(item.getId()), (int) submissions.countByAttemptItemId(item.getId()),
                    last.map(CodeSubmission::getStatus).orElse(null), last.map(CodeSubmission::getPassedTests).orElse(null),
                    last.map(CodeSubmission::getTotalTests).orElse(null), last.map(CodeSubmission::getLanguage).orElse(null),
                    last.map(CodeSubmission::getSourceCode).orElse(null));
            } else if (item.getEvaluationJson() != null) {
                try {
                    evaluation = objectMapper.readValue(item.getEvaluationJson(), EvaluationView.class);
                } catch (JsonProcessingException e) {
                    log.warn("Unreadable evaluation of item {}", item.getId());
                }
            }
            if (Boolean.TRUE.equals(item.getCorrect())) correctCount++;
            int[] t = topics.computeIfAbsent(topicOf(q), k -> new int[2]);
            t[0]++;
            if (Boolean.TRUE.equals(item.getCorrect())) t[1]++;
            items.add(new ResultItem(q.getId(), item.getSeqNo(), q.getType(), q.getTopic(), q.getTitle(), q.getText(), options, yourAnswer,
                item.getLanguage(), q.getExplanation(), item.getCorrect(), item.getAwardedScore() == null ? 0 : item.getAwardedScore(),
                item.getMaxScore(), evaluation, coding));
        }
        Long taken = a.getSubmittedAt() == null ? null : Math.min(Duration.between(a.getStartedAt(), a.getSubmittedAt()).getSeconds(), a.getDurationMinutes() * 60L);
        List<TopicPerformance> perf = topics.entrySet().stream().map(e -> new TopicPerformance(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
        return new ResultView(a.getId(), a.getRole().getCode(), a.getRole().getName(), a.getRound(), a.getRound().label(), a.getLevel(), a.getStatus(),
            a.getStartedAt(), a.getSubmittedAt(), taken, a.getScore() == null ? 0 : a.getScore(), a.getMaxScore(),
            a.getPercentage() == null ? 0 : a.getPercentage(), Boolean.TRUE.equals(a.getPassed()), a.getPassingPercentage(), a.getTotalQuestions(),
            correctCount, a.getSummaryFeedback(), scoringMethod(a.getRound()), perf, items);
    }

    private static String scoringMethod(InterviewRound round) {
        return switch (round) {
            case APTITUDE, TECHNICAL_MCQ -> "Each question is worth 1 mark and is marked on the server. A multiple-answer question needs exactly the right set of options. There is no negative marking.";
            case CODING -> "Each problem is worth 100 points: the share of test cases (visible and hidden) your best submission passed. Code runs in an external sandbox.";
            case TECHNICAL_INTERVIEW -> "Rule-based rubric, no AI: Correctness 35%, Completeness 25%, Clarity 20%, Technical understanding 20%. It checks which expected ideas your answer covers and how it is written; it cannot verify every claim, so compare with the model answer.";
            case HR_INTERVIEW -> "Rule-based rubric, no AI: Relevance 30%, Structure 25%, Specificity 25%, Communication 20%. It checks coverage of the points the question looks for, a STAR-style structure, concrete detail and clarity. Compare with the sample answer.";
        };
    }

    // ------------------------------------------------------------------ small utilities

    private static String topicOf(InterviewQuestion q) {
        return q.getTopic() != null ? q.getTopic() : (q.getTitle() != null ? q.getTitle() : "General");
    }

    static Set<Long> parseIds(String csv) {
        Set<Long> out = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) return out;
        for (String s : csv.split(",")) {
            try {
                out.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // skip bad value
            }
        }
        return out;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static EvaluationView toView(RubricEvaluator.Evaluation e) {
        return new EvaluationView(e.overall(), e.dimensions().stream().map(d -> new DimensionView(d.name(), d.score(), d.weightPercent(), d.comment())).toList(),
            e.strengths(), e.improvements(), e.matchedConcepts(), e.missedConcepts(), e.wordCount());
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise evaluation", e);
        }
    }
}
