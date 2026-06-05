package za.ac.alis.ai.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);

    /**
     * Extract text from raw bytes based on MIME type
     */
    public String extractTextFromBytes(byte[] bytes, String mimeType) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("File bytes are empty");
        }

        String lowerMime = mimeType != null ? mimeType.toLowerCase() : "";

        switch (lowerMime) {
            case "application/pdf":
                return extractFromPdf(bytes);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
            case "application/msword":
                return extractFromDocx(bytes);
            case "text/plain":
            case "text/csv":
            case "text/markdown":
                return extractFromText(bytes);
            default:
                log.warn("Unsupported MIME type: {}. Trying PDF as fallback.", lowerMime);
                try {
                    return extractFromPdf(bytes);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unsupported MIME type: " + lowerMime, e);
                }
        }
    }

    private String extractFromPdf(byte[] bytes) throws IOException {
        try (PDDocument pdf = PDDocument.load(new ByteArrayInputStream(bytes))) {
            if (pdf.isEncrypted()) {
                throw new IOException("PDF is password protected");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(Integer.MAX_VALUE);
            String rawText = stripper.getText(pdf);
            return cleanText(rawText);
        } catch (IOException e) {
            log.error("PDFBox extraction failed", e);
            throw e;
        }
    }

    private String extractFromDocx(byte[] bytes) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();
            docx.getParagraphs().forEach(paragraph -> {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    sb.append(text).append("\n");
                }
            });
            docx.getTables().forEach(table -> {
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.trim().isEmpty()) {
                            sb.append(cellText).append(" | ");
                        }
                    });
                    sb.append("\n");
                });
            });
            return cleanText(sb.toString());
        }
    }

    private String extractFromText(byte[] bytes) {
        return cleanText(new String(bytes, StandardCharsets.UTF_8));
    }

    private String cleanText(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        return raw
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .replaceAll("(\r?\n){3,}", "\n\n")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    public String extractTextFromPdfBytes(byte[] pdfBytes) throws IOException {
        return extractFromPdf(pdfBytes);
    }

    @Deprecated
    public String extractText(String fileUrl, String mimeType) throws Exception {
        log.warn("Deprecated extractText(String fileUrl, String mimeType) called.");
        byte[] bytes = downloadFileViaUrl(fileUrl);
        return extractTextFromBytes(bytes, mimeType);
    }

    private byte[] downloadFileViaUrl(String fileUrl) throws IOException, InterruptedException {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(fileUrl))
                .GET()
                .build();
        var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download file from URL, status: " + response.statusCode());
        }
        return response.body();
    }

    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
}