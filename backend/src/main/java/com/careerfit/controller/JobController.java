package com.careerfit.controller;

import com.careerfit.dto.CommonDtos.PageResponse;
import com.careerfit.dto.JobDtos.*;
import com.careerfit.entity.EmploymentType;
import com.careerfit.entity.ExperienceLevel;
import com.careerfit.entity.WorkMode;
import com.careerfit.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class JobController {

    private final JobService service;

    // ---- authenticated (personalised with match % and saved flags)
    @GetMapping("/api/jobs")
    public PageResponse<JobSummary> list(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String location,
                                         @RequestParam(required = false) WorkMode workMode,
                                         @RequestParam(required = false) ExperienceLevel experienceLevel,
                                         @RequestParam(required = false) EmploymentType employmentType,
                                         @RequestParam(required = false) Long careerPathId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(defaultValue = "posted") String sort,
                                         @RequestParam(defaultValue = "desc") String direction) {
        return service.search(true, new JobFilter(q, location, workMode, experienceLevel, employmentType, careerPathId),
            page, size, sort, direction);
    }

    @GetMapping("/api/jobs/matches")
    public PageResponse<JobSummary> matches(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return service.matches(page, size);
    }

    @GetMapping("/api/jobs/saved")
    public List<JobSummary> saved() {
        return service.savedJobs();
    }

    @GetMapping("/api/jobs/{id}")
    public JobDetail detail(@PathVariable Long id) {
        return service.detail(id, true);
    }

    @PostMapping("/api/jobs/{id}/save")
    public ResponseEntity<Void> save(@PathVariable Long id) {
        service.save(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/jobs/{id}/save")
    public ResponseEntity<Void> unsave(@PathVariable Long id) {
        service.unsave(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/jobs/{id}/application")
    public ApplicationDto upsertApplication(@PathVariable Long id, @Valid @RequestBody ApplicationRequest request) {
        return service.upsertApplication(id, request);
    }

    @DeleteMapping("/api/jobs/{id}/application")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        service.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/applications")
    public List<ApplicationDto> applications() {
        return service.myApplications();
    }

    // ---- public (no login, no personalisation)
    @GetMapping("/api/public/jobs")
    public PageResponse<JobSummary> publicList(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) String location,
                                               @RequestParam(required = false) WorkMode workMode,
                                               @RequestParam(required = false) ExperienceLevel experienceLevel,
                                               @RequestParam(required = false) EmploymentType employmentType,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return service.search(false, new JobFilter(q, location, workMode, experienceLevel, employmentType, null),
            page, size, "posted", "desc");
    }

    @GetMapping("/api/public/jobs/{id}")
    public JobDetail publicDetail(@PathVariable Long id) {
        return service.detail(id, false);
    }
}
