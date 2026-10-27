package com.careerfit.controller;

import com.careerfit.dto.ResumeDtos.*;
import com.careerfit.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeDto> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(file));
    }

    @GetMapping
    public List<ResumeDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ResumeDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/analyze")
    public AnalysisDto analyze(@PathVariable Long id, @RequestBody(required = false) AnalyzeRequest request) {
        return service.analyze(id, request == null ? null : request.careerPathId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
