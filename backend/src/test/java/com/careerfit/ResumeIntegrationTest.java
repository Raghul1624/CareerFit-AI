package com.careerfit;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeIntegrationTest extends IntegrationTestBase {

    private static byte[] pdf(String... lines) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.newLineAtOffset(40, 740);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -16);
                }
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private long upload(String token, byte[] bytes) throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "cv.pdf", "application/pdf", bytes);
        return json(mvc.perform(multipart("/api/resumes").file(f).header("Authorization", bearer(token)))
            .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    @Test
    void uploadAnalyzeAndDelete() throws Exception {
        String t = registerAndGetToken(uniqueEmail());
        long id = upload(t, pdf("Education B.E. Computer Science", "Skills Java SQL Spring Boot",
            "Projects Built an inventory system with Java and MySQL"));

        JsonNode analysis = json(mvc.perform(post("/api/resumes/" + id + "/analyze").header("Authorization", bearer(t))
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(Map.of("careerPathId", 1))))
            .andExpect(status().isOk()).andReturn());
        assertTrue(analysis.get("extractedSkills").toString().contains("Java Basics"));
        assertTrue(analysis.get("keywordCoverage").asDouble() > 0);
        assertTrue(analysis.get("disclaimer").asText().contains("not an ATS score"));

        assertEquals(1, json(mvc.perform(get("/api/resumes").header("Authorization", bearer(t))).andReturn()).size());
        assertFalse(json(mvc.perform(get("/api/resumes/" + id).header("Authorization", bearer(t)))
            .andExpect(status().isOk()).andReturn()).get("latestAnalysis").isNull());

        mvc.perform(delete("/api/resumes/" + id).header("Authorization", bearer(t))).andExpect(status().isNoContent());
        mvc.perform(get("/api/resumes/" + id).header("Authorization", bearer(t))).andExpect(status().isNotFound());
    }

    @Test
    void invalidUploadsAreRejected() throws Exception {
        String t = registerAndGetToken(uniqueEmail());
        // Fake PDF: right name and content type, wrong bytes.
        var fake = new MockMultipartFile("file", "cv.pdf", "application/pdf", "hello, not a pdf".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/resumes").file(fake).header("Authorization", bearer(t))).andExpect(status().isBadRequest());
        var wrongExt = new MockMultipartFile("file", "cv.txt", "application/pdf", pdf("Java"));
        mvc.perform(multipart("/api/resumes").file(wrongExt).header("Authorization", bearer(t))).andExpect(status().isBadRequest());
        var empty = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0]);
        mvc.perform(multipart("/api/resumes").file(empty).header("Authorization", bearer(t))).andExpect(status().isBadRequest());
        // Starts with %PDF- but is corrupt.
        var corrupt = new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.7 garbage".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/resumes").file(corrupt).header("Authorization", bearer(t))).andExpect(status().isBadRequest());
        assertEquals(0, json(mvc.perform(get("/api/resumes").header("Authorization", bearer(t))).andReturn()).size());
    }

    @Test
    void otherUsersCannotAccessAResume() throws Exception {
        String owner = registerAndGetToken(uniqueEmail());
        String intruder = registerAndGetToken(uniqueEmail());
        long id = upload(owner, pdf("Java developer"));

        mvc.perform(get("/api/resumes/" + id).header("Authorization", bearer(intruder))).andExpect(status().isNotFound());
        mvc.perform(post("/api/resumes/" + id + "/analyze").header("Authorization", bearer(intruder))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/resumes/" + id).header("Authorization", bearer(intruder))).andExpect(status().isNotFound());
        assertEquals(0, json(mvc.perform(get("/api/resumes").header("Authorization", bearer(intruder))).andReturn()).size());
        mvc.perform(get("/api/resumes/" + id).header("Authorization", bearer(owner))).andExpect(status().isOk());
    }

    @Test
    void resumeEndpointsRequireLogin() throws Exception {
        mvc.perform(get("/api/resumes")).andExpect(status().isUnauthorized());
    }
}
