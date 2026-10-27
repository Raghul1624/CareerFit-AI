package com.careerfit.service;

import com.careerfit.dto.CommonDtos.PageResponse;
import com.careerfit.dto.JobDtos.*;
import com.careerfit.entity.*;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.*;
import com.careerfit.security.CurrentUser;
import com.careerfit.util.SafeUrl;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Job discovery, rule-based matching, saved jobs and application tracking.
 *
 * Match formula (documented in README):
 *   requiredPart  = matched required skills / total required skills
 *   preferredPart = matched preferred skills / total preferred skills
 *   percent = 100 * (w * requiredPart + (1 - w) * preferredPart), w = app.matching.required-weight (default 0.7)
 *   If a job has only one kind of skill, that part counts for 100%. Jobs without skills score 0.
 * A skill counts as "yours" when you declared it OR scored >= 70% on it in finished tests.
 *
 * Every private record (saved job, application) is looked up by (current user, job id); ids of other
 * users' rows are never accepted from the client.
 */
@Service
@RequiredArgsConstructor
public class JobService {

    private static final Map<String, String> SORT_FIELDS = Map.of("posted", "postedAt", "title", "title", "company", "company");

    private final CurrentUser currentUser;
    private final JobRepository jobs;
    private final SavedJobRepository saved;
    private final JobApplicationRepository applications;
    private final CareerPathRepository paths;
    private final TestAttemptRepository attempts;

    @Value("${app.matching.required-weight:0.7}")
    private double requiredWeight;

    private record Ctx(boolean personalized, Set<String> skills, Set<Long> savedIds) {}

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<JobSummary> search(boolean personalized, JobFilter f, int page, int size, String sort, String direction) {
        Set<Long> skillIds = null;
        if (f.careerPathId() != null) {
            CareerPath path = paths.findByIdAndActiveTrue(f.careerPathId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown career path"));
            skillIds = path.getSkills().stream().map(Skill::getId).collect(Collectors.toSet());
        }
        String property = SORT_FIELDS.getOrDefault(sort == null ? "posted" : sort, "postedAt");
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(dir, property));
        Ctx ctx = context(personalized);
        var result = jobs.findAll(spec(f, skillIds), pr);
        return PageResponse.from(result.map(j -> summary(j, ctx)));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummary> matches(int page, int size) {
        Ctx ctx = context(true);
        List<JobSummary> all = jobs.findByActiveTrue().stream().map(j -> summary(j, ctx))
            .sorted(Comparator.comparing((JobSummary s) -> s.matchPercent() == null ? 0 : s.matchPercent()).reversed()
                .thenComparing(JobSummary::postedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
        int sz = Math.min(Math.max(size, 1), 50);
        int pg = Math.max(page, 0);
        int from = Math.min(pg * sz, all.size());
        int to = Math.min(from + sz, all.size());
        return new PageResponse<>(all.subList(from, to), pg, sz, all.size(), (int) Math.ceil(all.size() / (double) sz));
    }

    @Transactional(readOnly = true)
    public List<JobSummary> savedJobs() {
        User user = currentUser.get();
        Ctx ctx = context(true);
        return saved.findByUserIdOrderBySavedAtDesc(user.getId()).stream()
            .map(SavedJob::getJob).filter(Job::isActive).map(j -> summary(j, ctx)).toList();
    }

    @Transactional(readOnly = true)
    public JobDetail detail(Long id, boolean personalized) {
        Job job = jobs.findByIdAndActiveTrue(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));
        Ctx ctx = context(personalized);
        MatchInfo match = personalized ? match(job, ctx.skills()) : null;
        ApplicationDto app = null;
        if (personalized) {
            app = applications.findByUserIdAndJobId(currentUser.get().getId(), id).map(this::toDto).orElse(null);
        }
        return new JobDetail(job.getId(), job.getTitle(), job.getCompany(), job.getDescription(), job.getLocation(),
            job.getWorkMode(), job.getExperienceLevel(), job.getEmploymentType(),
            SafeUrl.sanitize(job.getApplicationUrl()), job.getPostedAt(), job.isSample(),
            names(job, true), names(job, false), match, ctx.savedIds().contains(id), app);
    }

    // ------------------------------------------------------------------ saved jobs

    @Transactional
    public void save(Long jobId) {
        User user = currentUser.get();
        Job job = jobs.findByIdAndActiveTrue(jobId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));
        if (!saved.existsByUserIdAndJobId(user.getId(), jobId)) {
            SavedJob s = new SavedJob();
            s.setUser(user);
            s.setJob(job);
            saved.save(s);
        }
    }

    @Transactional
    public void unsave(Long jobId) {
        saved.deleteByUserIdAndJobId(currentUser.get().getId(), jobId);
    }

    // ------------------------------------------------------------------ applications

    @Transactional(readOnly = true)
    public List<ApplicationDto> myApplications() {
        return applications.findByUserIdOrderByUpdatedAtDesc(currentUser.get().getId()).stream().map(this::toDto).toList();
    }

    @Transactional
    public ApplicationDto upsertApplication(Long jobId, ApplicationRequest request) {
        User user = currentUser.get();
        Job job = jobs.findByIdAndActiveTrue(jobId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));
        JobApplication app = applications.findByUserIdAndJobId(user.getId(), jobId).orElseGet(() -> {
            JobApplication a = new JobApplication();
            a.setUser(user);
            a.setJob(job);
            return a;
        });
        app.setStatus(request.status());
        app.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        app.setUpdatedAt(Instant.now());
        return toDto(applications.save(app));
    }

    @Transactional
    public void deleteApplication(Long jobId) {
        applications.deleteByUserIdAndJobId(currentUser.get().getId(), jobId);
    }

    // ------------------------------------------------------------------ helpers

    private Ctx context(boolean personalized) {
        if (!personalized) return new Ctx(false, Set.of(), Set.of());
        User user = currentUser.get();
        Set<String> skills = user.getDeclaredSkills().stream().map(Skill::getName).collect(Collectors.toCollection(HashSet::new));
        for (Object[] row : attempts.skillPerformance(user.getId(), AttemptStatus.IN_PROGRESS)) {
            long correct = row[1] == null ? 0 : ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            if (total > 0 && correct * 100.0 / total >= AnalyticsService.STRONG_THRESHOLD) skills.add((String) row[0]);
        }
        Set<Long> savedIds = saved.findByUserIdOrderBySavedAtDesc(user.getId()).stream()
            .map(s -> s.getJob().getId()).collect(Collectors.toSet());
        return new Ctx(true, skills, savedIds);
    }

    private JobSummary summary(Job j, Ctx ctx) {
        Double pct = ctx.personalized() ? match(j, ctx.skills()).percent() : null;
        return new JobSummary(j.getId(), j.getTitle(), j.getCompany(), j.getLocation(), j.getWorkMode(),
            j.getExperienceLevel(), j.getEmploymentType(), j.getPostedAt(), j.isSample(),
            names(j, true), pct, ctx.savedIds().contains(j.getId()));
    }

    private static List<String> names(Job j, boolean required) {
        return j.getSkills().stream().filter(s -> s.isRequired() == required)
            .map(s -> s.getSkill().getName()).sorted().toList();
    }

    MatchInfo match(Job j, Set<String> userSkills) {
        List<String> req = names(j, true);
        List<String> pref = names(j, false);
        List<String> matchedReq = req.stream().filter(userSkills::contains).toList();
        List<String> matchedPref = pref.stream().filter(userSkills::contains).toList();
        double reqPart = req.isEmpty() ? 0 : matchedReq.size() / (double) req.size();
        double prefPart = pref.isEmpty() ? 0 : matchedPref.size() / (double) pref.size();
        double pct;
        if (req.isEmpty() && pref.isEmpty()) pct = 0;
        else if (pref.isEmpty()) pct = reqPart * 100;
        else if (req.isEmpty()) pct = prefPart * 100;
        else pct = (requiredWeight * reqPart + (1 - requiredWeight) * prefPart) * 100;
        List<String> matched = new ArrayList<>(matchedReq);
        matched.addAll(matchedPref);
        return new MatchInfo(Math.round(pct * 10.0) / 10.0, matched,
            req.stream().filter(s -> !userSkills.contains(s)).toList(),
            pref.stream().filter(s -> !userSkills.contains(s)).toList());
    }

    private ApplicationDto toDto(JobApplication a) {
        return new ApplicationDto(a.getJob().getId(), a.getJob().getTitle(), a.getJob().getCompany(),
            a.getStatus(), a.getNotes(), a.getUpdatedAt());
    }

    private static Specification<Job> spec(JobFilter f, Set<Long> skillIds) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(root.get("active")));
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + escape(f.q().trim().toLowerCase()) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("title")), like, '\\'),
                    cb.like(cb.lower(root.get("company")), like, '\\')));
            }
            if (f.location() != null && !f.location().isBlank()) {
                ps.add(cb.like(cb.lower(root.get("location")), "%" + escape(f.location().trim().toLowerCase()) + "%", '\\'));
            }
            if (f.workMode() != null) ps.add(cb.equal(root.get("workMode"), f.workMode()));
            if (f.experienceLevel() != null) ps.add(cb.equal(root.get("experienceLevel"), f.experienceLevel()));
            if (f.employmentType() != null) ps.add(cb.equal(root.get("employmentType"), f.employmentType()));
            if (skillIds != null) {
                if (skillIds.isEmpty()) {
                    ps.add(cb.disjunction());
                } else {
                    query.distinct(true);
                    Join<Job, JobSkill> js = root.join("skills");
                    ps.add(js.get("id").get("skillId").in(skillIds));
                }
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
