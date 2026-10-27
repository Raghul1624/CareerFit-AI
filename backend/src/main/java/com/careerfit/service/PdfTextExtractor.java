package com.careerfit.service;

import com.careerfit.exception.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfTextExtractor {

    public record Extracted(String text, int pageCount) {}

    private static final int MAX_CHARS = 200_000;

    private final int maxPages;

    public PdfTextExtractor(@Value("${app.resume.max-pages:10}") int maxPages) {
        this.maxPages = maxPages;
    }

    /** Parses the PDF (never executes anything inside it) and returns plain text. */
    public Extracted extract(byte[] bytes) {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            int pages = doc.getNumberOfPages();
            if (pages == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "The PDF has no pages");
            if (pages > maxPages) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "The PDF has too many pages (maximum " + maxPages + ")");
            }
            String text = new PDFTextStripper().getText(doc);
            if (text.length() > MAX_CHARS) text = text.substring(0, MAX_CHARS);
            return new Extracted(text, pages);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This file could not be read as a PDF (it may be corrupt or password protected)");
        }
    }
}
