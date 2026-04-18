package za.ac.alis.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI PIPELINE — LAYER 1: Text Extraction Service
 *
 * Responsible for reading the raw uploaded file from disk
 * and extracting clean plain text for AI processing.
 *
 * Supports:
 *   - PDF         (.pdf)  → Apache PDFBox
 *   - Word DOCX   (.docx) → Apache POI
 *   - Plain text  (.txt)  → direct read
 *   - CSV         (.csv)  → direct read
 *
 * Output feeds into:
 *   → DocumentContent.extractedText
 *   → EmbeddingService (next layer)
 */
@Service
public class TextExtractionService {

    private static final String STORAGE_BASE = "uploads";

    /**
     * Extracts plain text from a stored document file.
     *
     * @param relativePath  stored path from FileMetadata.storagePath
     * @param mimeType      stored MIME type from FileMetadata.mimeType
     * @return extracted plain text (never null, may be empty)
     */
    public String extractText(String relativePath, String mimeType) throws IOException {
        Path fullPath = Paths.get(STORAGE_BASE, relativePath);

        if (!Files.exists(fullPath)) {
            throw new IOException("File not found on disk: " + fullPath);
        }

        byte[] fileBytes = Files.readAllBytes(fullPath);

        return switch (mimeType.toLowerCase()) {
            case "application/pdf"      -> extractFromPdf(fileBytes);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                        -> extractFromDocx(fileBytes);
            case "text/plain",
                 "text/csv"             -> extractFromText(fileBytes);
            default -> throw new IllegalArgumentException(
                    "Unsupported MIME type for extraction: " + mimeType);
        };
    }

    // ── PDF Extraction ────────────────────────────────────────────────────────
    private String extractFromPdf(byte[] bytes) throws IOException {
        try (PDDocument pdf = PDDocument.load(new ByteArrayInputStream(bytes))) {
            if (pdf.isEncrypted()) {
                throw new IOException("PDF is encrypted and cannot be read.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);  // preserve reading order
            return cleanText(stripper.getText(pdf));
        }
    }

    // ── DOCX Extraction ───────────────────────────────────────────────────────
    private String extractFromDocx(byte[] bytes) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            List<XWPFParagraph> paragraphs = docx.getParagraphs();
            String text = paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining("\n"));
            return cleanText(text);
        }
    }

    // ── Plain Text / CSV Extraction ───────────────────────────────────────────
    private String extractFromText(byte[] bytes) {
        return cleanText(new String(bytes, StandardCharsets.UTF_8));
    }

    // ── Text Cleaning ─────────────────────────────────────────────────────────
    /**
     * Normalizes extracted text:
     *  - Removes non-printable characters
     *  - Collapses excessive blank lines (max 2 in a row)
     *  - Trims leading/trailing whitespace
     */
    private String cleanText(String raw) {
        if (raw == null) return "";

        return raw
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "") // strip control chars
            .replaceAll("(\r?\n){3,}", "\n\n")                           // collapse blank lines
            .trim();
    }
}
