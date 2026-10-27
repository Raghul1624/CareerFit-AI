package com.careerfit.service;

import com.careerfit.dto.CommonDtos.PageResponse;
import com.careerfit.dto.TestDtos.*;
import com.careerfit.entity.*;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.MockTestRepository;
import com.careerfit.repository.QuestionRef;
import com.careerfit.repository.QuestionRepository;
import com.careerfit.repository.TestAttemptRepository;
import com.careerfit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
 * Mock test engine. The server is the single source of truth for: question selection,
 * time limits, scoring and attempt state. The client never sees correct answers before submission.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttemptService {

    /** Small allowance for network latency when a submit arrives right at the deadline. */
    private static final long GRACE_SECONDS = 5;
    private static final Map<String, String> SORT_FIELDS = Map.of(
        "date", "startedAt", "score", "percentage", "title", "mockTest.title");

    private final CurrentUser currentUser;
    private final MockTestRepository tests;
    private final QuestionRepository questions;
    private final TestAttemptRepository attempts;

    // ------------------------------------------------------------------ start

    @Transactional
    public AttemptView start(Long testId) {
        User user = currentUser.get();
        MockTest test = tests.findById(testId).filter(MockTest::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test not found"));
        Instant now = Instant.now();

        // Resume an unfinished attempt instead of creating duplicates.
        Optional<TestAttempt> open = attempts.findFirstByUserIdAndMockTestIdAndStatusOrderByStartedAtDesc(
            user.getId(), testId, AttemptStatus.IN_PROGRESS);
        if (open.isPresent()) {
            TestAttempt existing = open.get();
            if (!isExpired(existing, now)) {
                return toView(existing, now);
            }
            finalizeAttempt(existing, AttemptStatus.AUTO_SUBMITTED);
        }

        if (test.getMaxAttempts() != null
            && attempts.countByUserIdAndMockTestId(user.getId(), testId) >= test.getMaxAttempts()) {
            throw new ApiException(HttpStatus.CONFLICT, "Maximum attempts reached for this test");
        }

        List<Question> selected = selectQuestions(test, user);

        TestAttempt attempt = new TestAttempt();
        attempt.setUser(user);
        attempt.setMockTest(test);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(now);
        attempt.setExpiresAt(now.plus(Duration.ofMinutes(test.getDurationMinutes())));
        attempt.setDurationMinutes(test.getDurationMinutes());
        attempt.setMarksPerQuestion(test.getMarksPerQuestion());
        attempt.setNegativeMarks(test.getNegativeMarks());
        attempt.setPassingPercentage(test.getPassingPercentage());
        attempt.setTotalQuestions(selected.size());
        attempt.setMaxScore(selected.size() * test.getMarksPerQuestion());
        for (int i = 0; i < selected.size(); i++) {
            TestAttemptQuestion item = new TestAttemptQuestion();
            item.setAttempt(attempt);
            item.setQuestion(selected.get(i));
            item.setSeqNo(i + 1);          // order is persisted: a refresh never reshuffles the test
            attempt.getItems().add(item);
        }
        attempts.save(attempt);
        return toView(attempt, now);
    }

    /**
     * Picks the questions for a new attempt.
     * <ol>
     *   <li>Eligible skills: a skill test uses its skill; a career test uses only skills that belong to its
     *       career path ({@link MockTest#eligibleSkillIds()}).</li>
     *   <li>If the pool is smaller than the configured question count the request fails with a clear 409.
     *       A test is never silently shortened or filled with questions from another skill.</li>
     *   <li>{@link QuestionSelector} balances skills, topics and difficulty at random and prefers questions
     *       the user has not seen in earlier attempts of this test.</li>
     *   <li>Only the chosen questions are loaded with their options, in a single query.</li>
     * </ol>
     */
    private List<Question> selectQuestions(MockTest test, User user) {
        Set<Long> skillIds = test.eligibleSkillIds();
        if (skillIds.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "This test has no skills configured yet");
        }
        List<QuestionRef> pool = questions.findPool(skillIds);
        if (pool.size() < test.getQuestionCount()) {
            throw new ApiException(HttpStatus.CONFLICT, "This test is not available yet: it needs "
                + test.getQuestionCount() + " questions but only " + pool.size() + " are available");
        }
        Set<Long> seen = new HashSet<>(attempts.findQuestionIdsSeenByUser(user.getId(), test.getId()));
        List<Long> chosen = QuestionSelector.select(pool, test.getQuestionCount(), seen, ThreadLocalRandom.current());

        Map<Long, Question> byId = questions.findAllWithOptionsByIdIn(chosen).stream()
            .collect(Collectors.toMap(Question::getId, q -> q));
        List<Question> result = new ArrayList<>(chosen.size());
        for (Long id : chosen) {
            Question q = byId.get(id);
            if (q == null) {
                throw new ApiException(HttpStatus.CONFLICT, "A selected question is no longer available; please try again");
            }
            result.add(q);
        }
        return result;
    }

    // ------------------------------------------------------------------ active attempt

    @Transactional(noRollbackFor = ApiException.class)
    public AttemptView get(Long id) {
        TestAttempt a = owned(id);
        Instant now = Instant.now();
        requireOpen(a, now);
        return toView(a, now);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void saveAnswers(Long id, SaveAnswersRequest request) {
        TestAttempt a = owned(id);
        requireOpen(a, Instant.now());
        Map<Long, TestAttemptQuestion> byQuestion = a.getItems().stream()
            .collect(Collectors.toMap(i -> i.getQuestion().getId(), i -> i));
        for (AnswerItem answer : request.answers()) {
            TestAttemptQuestion item = byQuestion.get(answer.questionId());
            if (item == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Question does not belong to this attempt");
            }
            List<Long> selected = answer.selectedOptionIds() == null ? List.of() : answer.selectedOptionIds();
            Set<Long> validIds = item.getQuestion().getOptions().stream()
                .map(QuestionOption::getId).collect(Collectors.toSet());
            if (!validIds.containsAll(selected)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid option selected");
            }
            if (item.getQuestion().getType() != QuestionType.MULTIPLE_CHOICE && selected.size() > 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only one option may be selected for this question");
            }
            item.setSelectedOptionIds(selected.isEmpty() ? null
                : new TreeSet<>(selected).stream().map(String::valueOf).collect(Collectors.joining(",")));
            item.setMarkedForReview(answer.markedForReview());
        }
    }

    @Transactional
    public ResultView submit(Long id) {
        TestAttempt a = owned(id);
        Instant now = Instant.now();
        if (a.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "This attempt has already been submitted");
        }
        finalizeAttempt(a, isExpired(a, now) ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);
        return toResult(a);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public ResultView result(Long id) {
        TestAttempt a = owned(id);
        if (a.getStatus() == AttemptStatus.IN_PROGRESS) {
            if (isExpired(a, Instant.now())) {
                finalizeAttempt(a, AttemptStatus.AUTO_SUBMITTED);
            } else {
                throw new ApiException(HttpStatus.CONFLICT, "Results are available after you submit the test");
            }
        }
        return toResult(a);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttemptSummary> history(int page, int size, String sort, String direction) {
        User user = currentUser.get();
        String property = SORT_FIELDS.getOrDefault(sort == null ? "date" : sort, "startedAt");
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(dir, property));
        return PageResponse.from(attempts.findByUserId(user.getId(), pr).map(this::toSummary));
    }

    /** Safety net: finalizes attempts whose time ran out and were never submitted. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sweepExpired() {
        Instant cutoff = Instant.now().minusSeconds(GRACE_SECONDS);
        for (TestAttempt a : attempts.findByStatusAndExpiresAtBefore(AttemptStatus.IN_PROGRESS, cutoff)) {
            finalizeAttempt(a, AttemptStatus.AUTO_SUBMITTED);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Ownership check: another user's attempt looks exactly like a missing one (404). */
    private TestAttempt owned(Long id) {
        User user = currentUser.get();
        return attempts.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
    }

    private boolean isExpired(TestAttempt a, Instant now) {
        return now.isAfter(a.getExpiresAt().plusSeconds(GRACE_SECONDS));
    }

    private void requireOpen(TestAttempt a, Instant now) {
        if (a.getStatus() == AttemptStatus.IN_PROGRESS && isExpired(a, now)) {
            finalizeAttempt(a, AttemptStatus.AUTO_SUBMITTED);
        }
        if (a.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "This attempt has already been submitted");
        }
    }

    private void finalizeAttempt(TestAttempt a, AttemptStatus status) {
        int correct = 0, incorrect = 0, unanswered = 0;
        double score = 0;
        for (TestAttemptQuestion item : a.getItems()) {
            Set<Long> selected = parseIds(item.getSelectedOptionIds());
            Set<Long> right = item.getQuestion().getOptions().stream()
                .filter(QuestionOption::isCorrect).map(QuestionOption::getId).collect(Collectors.toSet());
            if (selected.isEmpty()) {
                unanswered++;
                item.setCorrect(null);
                item.setAwardedMarks(0.0);
            } else if (selected.equals(right)) {
                correct++;
                item.setCorrect(true);
                item.setAwardedMarks(a.getMarksPerQuestion());
                score += a.getMarksPerQuestion();
            } else {
                incorrect++;
                item.setCorrect(false);
                item.setAwardedMarks(-a.getNegativeMarks());
                score -= a.getNegativeMarks();
            }
        }
        score = Math.max(0, score);
        double percentage = a.getMaxScore() > 0 ? round2(score / a.getMaxScore() * 100) : 0;
        a.setStatus(status);
        a.setSubmittedAt(status == AttemptStatus.AUTO_SUBMITTED ? a.getExpiresAt() : Instant.now());
        a.setScore(round2(score));
        a.setCorrectCount(correct);
        a.setIncorrectCount(incorrect);
        a.setUnansweredCount(unanswered);
        a.setPercentage(percentage);
        a.setPassed(percentage >= a.getPassingPercentage());
    }

    private static Set<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(",")).map(Long::valueOf).collect(Collectors.toSet());
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private AttemptView toView(TestAttempt a, Instant now) {
        List<PublicQuestion> qs = a.getItems().stream().map(i -> {
            Question q = i.getQuestion();
            List<PublicOption> options = q.getOptions().stream()
                .map(o -> new PublicOption(o.getId(), o.getText())).toList();
            return new PublicQuestion(q.getId(), i.getSeqNo(), q.getText(), q.getType(), options,
                new ArrayList<>(new TreeSet<>(parseIds(i.getSelectedOptionIds()))), i.isMarkedForReview());
        }).toList();
        return new AttemptView(a.getId(), a.getMockTest().getId(), a.getMockTest().getTitle(), a.getStatus(),
            a.getStartedAt(), a.getExpiresAt(), now, a.getDurationMinutes(), a.getTotalQuestions(), qs,
            a.getMockTest().getMode(), careerPathId(a.getMockTest()), careerPathName(a.getMockTest()));
    }

    private AttemptSummary toSummary(TestAttempt a) {
        Long taken = a.getSubmittedAt() == null ? null
            : Duration.between(a.getStartedAt(), a.getSubmittedAt()).toSeconds();
        return new AttemptSummary(a.getId(), a.getMockTest().getId(), a.getMockTest().getTitle(), a.getStatus(),
            a.getStartedAt(), a.getSubmittedAt(), a.getScore(), a.getMaxScore(), a.getPercentage(),
            a.getPassed(), taken, a.getTotalQuestions(),
            a.getMockTest().getMode(), careerPathId(a.getMockTest()), careerPathName(a.getMockTest()));
    }

    private static Long careerPathId(MockTest t) {
        return t.getCareerPath() == null ? null : t.getCareerPath().getId();
    }

    private static String careerPathName(MockTest t) {
        return t.getCareerPath() == null ? null : t.getCareerPath().getName();
    }

    public AttemptSummary summaryOf(TestAttempt a) {
        return toSummary(a);
    }

    private ResultView toResult(TestAttempt a) {
        List<ReviewItem> review = new ArrayList<>();
        Map<String, int[]> topics = new LinkedHashMap<>();   // topic -> [correct, total]
        for (TestAttemptQuestion i : a.getItems()) {
            Question q = i.getQuestion();
            Set<Long> selected = parseIds(i.getSelectedOptionIds());
            String topic = q.getTopic() != null ? q.getTopic() : q.getSkill().getName();
            int[] stat = topics.computeIfAbsent(topic, k -> new int[2]);
            stat[1]++;
            if (Boolean.TRUE.equals(i.getCorrect())) stat[0]++;
            List<ReviewOption> options = q.getOptions().stream()
                .map(o -> new ReviewOption(o.getId(), o.getText(), o.isCorrect(), selected.contains(o.getId()))).toList();
            review.add(new ReviewItem(q.getId(), i.getSeqNo(), q.getText(), topic, q.getType(), options,
                i.getCorrect(), !selected.isEmpty(), q.getExplanation(), i.getAwardedMarks()));
        }
        List<TopicStat> topicStats = topics.entrySet().stream()
            .map(e -> new TopicStat(e.getKey(), e.getValue()[0], e.getValue()[1],
                round2(e.getValue()[0] * 100.0 / e.getValue()[1]))).toList();
        List<String> revision = topicStats.stream().filter(t -> t.accuracy() < 60)
            .map(TopicStat::topic).toList();
        long taken = Duration.between(a.getStartedAt(), a.getSubmittedAt()).toSeconds();
        MockTest t = a.getMockTest();
        boolean canRetake = t.getMaxAttempts() == null
            || attempts.countByUserIdAndMockTestId(a.getUser().getId(), t.getId()) < t.getMaxAttempts();
        return new ResultView(a.getId(), t.getId(), t.getTitle(), a.getStatus(), a.getStartedAt(), a.getSubmittedAt(),
            a.getScore(), a.getMaxScore(), a.getPercentage(), Boolean.TRUE.equals(a.getPassed()),
            a.getPassingPercentage(), taken, a.getTotalQuestions(), a.getCorrectCount(), a.getIncorrectCount(),
            a.getUnansweredCount(), topicStats, review, revision, canRetake,
            t.getMode(), careerPathId(t), careerPathName(t));
    }
}
