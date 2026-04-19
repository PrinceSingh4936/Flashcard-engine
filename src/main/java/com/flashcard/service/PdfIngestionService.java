package com.flashcard.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PdfIngestionService {

    private static final int MAX_PAGES_PER_CHUNK = 3;
    private static final int MAX_TOTAL_PAGES = 30; // cap to avoid OOM on huge PDFs

    public String extractText(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(Math.min(document.getNumberOfPages(), MAX_TOTAL_PAGES));
                String raw = stripper.getText(document);
                return cleanText(raw);
            }
        } catch (IOException e) {
            log.error("PDF extraction failed", e);
            throw new RuntimeException("Could not read PDF: " + e.getMessage());
        }
    }

    /**
     * Chunks by pages — 3 pages per chunk max.
     * Avoids loading the entire text into memory at once.
     */
    public List<String> chunkText(MultipartFile file) {
        List<String> chunks = new ArrayList<>();

        try {
            byte[] bytes = file.getBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                int totalPages = Math.min(document.getNumberOfPages(), MAX_TOTAL_PAGES);
                PDFTextStripper stripper = new PDFTextStripper();

                for (int start = 1; start <= totalPages; start += MAX_PAGES_PER_CHUNK) {
                    int end = Math.min(start + MAX_PAGES_PER_CHUNK - 1, totalPages);
                    stripper.setStartPage(start);
                    stripper.setEndPage(end);
                    String pageText = cleanText(stripper.getText(document));
                    if (!pageText.isBlank()) {
                        chunks.add(pageText);
                    }
                }
            }
        } catch (IOException e) {
            log.error("PDF chunking failed", e);
            throw new RuntimeException("Could not chunk PDF: " + e.getMessage());
        }

        log.info("PDF split into {} chunks ({} pages max)", chunks.size(), MAX_TOTAL_PAGES);
        return chunks;
    }

    private String cleanText(String raw) {
        return raw
                .replaceAll("\\r\\n|\\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[^\\x20-\\x7E\\n]", " ")
                .trim();
    }
}