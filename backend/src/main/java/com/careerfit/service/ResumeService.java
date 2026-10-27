package com.careerfit.service;

import com.careerfit.dto.ResumeDtos.*;
import com.careerfit.entity.*;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.*;
import com.careerfit.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resume upload and analysis. Every lookup is by (current user, resume id), so another user's resume
 * looks exactly like a missing one (404). Resume text is never logged and never stored; only derived
 * skill lists and suggestions are saved. Files stay until the user deletes the resume.
 */
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    private final CurrentUser currentUser;
    private final ResumeRepository resumes;
    private final ResumeAnalysisRepository analyses;
    private final SkillRepository skills;
    private final CareerPathRepository paths;
    private final UserProfileRepository profiles;
    private final ResumeStorage storage;
    private final PdfTextExtractor extractor;
    private final ResumeAnalyzer analyzer;
    private final ObjectMapper mapper;
    private final ObjectProvider<AiFeedbackProvider> aiProviders;

    @Value("${app.resume.max-bytes:2097152}")
    private long maxBytes;

    @Value("${app.resume.max-per-user:5}")
    private int maxPerUser;

    @Transactional
    public ResumeDto upload(MultipartFile file) {
        User user = currentUser.get();
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose a PDF file");
        if (file.getSize() > maxBytes) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "The file is too large (maximum 2 MB)");
        String original = sanitizeName(file.getOriginalFilename());
        if (!original.toLowerCase().endsWith(".pdf")) throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted");
        if (resumes.countByUserId(user.getId()) >= maxPerUser) {
            throw new ApiException(HttpStatus.CONFLICT, "You can keep at most " + maxPerUser + " resumes. Delete one first.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The file could not be read");
        }
        // Content-Type and extension are client-controlled; verify the real file signature and parseability.
        if (bytes.length < PDF_MAGIC.length) throw new ApiException(HttpStatus.BAD_REQUEST, "The file is not a valid PDF");
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) throw new ApiException(HttpStatus.BAD_REQUEST, "The file is not a valid PDF");
        }
        extractor.extract(bytes);   // rejects corrupt, encrypted-with-password and over-long PDFs

        Resume r = new Resume();
        r.setUser(user);
        r.setOriginalFilename(original);
        r.setStoredFilename(storage.store(bytes));
        r.setSizeBytes(bytes.length);
        return toDto(resumes.save(r));
    }

    @Transactional(readOnly = true)
    public List<ResumeDto> list() {
        return resumes.findByUserIdOrderByUploadedAtDesc(currentUser.get().getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ResumeDetail get(Long id) {
        Resume r = owned(id);
        AnalysisDto latest = analyses.findFirstByResumeIdOrderByCreatedAtDesc(r.getId()).map(a -> toAnalysisDto(a, null)).orElse(null);
        return new ResumeDetail(r.getId(), r.getOriginalFilename(), r.getSizeBytes(), r.getUploadedAt(), latest);
    }

    @Transactional
    public AnalysisDto analyze(Long id, Long careerPathId) {
        User user = currentUser.get();
        Resume r = owned(id);

        CareerPath path = null;
        if (careerPathId != null) {
            path = paths.findByIdAndActiveTrue(careerPathId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown career path"));
        } else {
            path = profiles.findByUserId(user.getId()).map(UserProfile::getTargetCareerPath).orElse(null);
        }
        List<String> careerSkills = path == null ? List.of()
            : path.getSkills().stream().map(Skill::getName).sorted().toList();

        PdfTextExtractor.Extracted extracted = extractor.extract(storage.read(r.getStoredFilename()));
        List<String> known = skills.findAll().stream().map(Skill::getName).toList();
        ResumeAnalyzer.Result result = analyzer.analyze(extracted.text(), extracted.pageCount(), known, careerSkills);

        ResumeAnalysis a = new ResumeAnalysis();
        a.setResume(r);
        a.setCareerPath(path);
        a.setKeywordCoverage(result.coverage());
        a.setExtractedSkills(json(result.extractedSkills()));
        a.setMissingSkills(json(result.missingSkills()));
        a.setSuggestions(json(result.suggestions()));
        a.setWordCount(result.wordCount());
        a.setPageCount(result.pageCount());
        analyses.save(a);

        List<String> ai = null;
        if (path != null) {
            Optional<AiFeedbackProvider> provider = aiProviders.orderedStream().findFirst();
            if (provider.isPresent()) {
                try {
                    ai = provider.get().feedback(path.getName(), result.extractedSkills(), result.missingSkills()).orElse(null);
                } catch (RuntimeException e) {
                    ai = null;   // AI is optional; the rule-based result is still returned
                }
            }
        }
        return toAnalysisDto(a, ai);
    }

    @Transactional
    public void delete(Long id) {
        Resume r = owned(id);
        String stored = r.getStoredFilename();
        resumes.delete(r);          // analyses are removed by the database (ON DELETE CASCADE)
        resumes.flush();
        storage.delete(stored);
    }

    // ------------------------------------------------------------------ helpers

    private Resume owned(Long id) {
        return resumes.findByIdAndUserId(id, currentUser.get().getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resume not found"));
    }

    private static String sanitizeName(String name) {
        String n = name == null ? "resume.pdf" : name;
        n = n.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}<>\"|?*:]", "").trim();
        if (n.length() > 120) n = n.substring(n.length() - 120);
        return n.isEmpty() ? "resume.pdf" : n;
    }

    private ResumeDto toDto(Resume r) {
        Optional<ResumeAnalysis> latest = analyses.findFirstByResumeIdOrderByCreatedAtDesc(r.getId());
        return new ResumeDto(r.getId(), r.getOriginalFilename(), r.getSizeBytes(), r.getUploadedAt(),
            latest.map(ResumeAnalysis::getCreatedAt).orElse(null),
            latest.map(ResumeAnalysis::getKeywordCoverage).orElse(null));
    }

    private AnalysisDto toAnalysisDto(ResumeAnalysis a, List<String> ai) {
        CareerPath p = a.getCareerPath();
        return new AnalysisDto(a.getId(), a.getCreatedAt(), p == null ? null : p.getId(), p == null ? null : p.getName(),
            a.getKeywordCoverage(), fromJson(a.getExtractedSkills()), fromJson(a.getMissingSkills()),
            fromJson(a.getSuggestions()), a.getWordCount() == null ? 0 : a.getWordCount(),
           a.getPageCount() == null ? 0 : a.getPageCount(),
com.careerfit.dto.ResumeDtos.DISCLAIMER, ai);
    }

    private String json(List<String> list) {
        try {
            return mapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> fromJson(String s) {
        if (s == null || s.isBlank()) return new ArrayList<>();
        try {
            return mapper.readValue(s, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
