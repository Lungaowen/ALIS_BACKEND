package za.ac.alis.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class TextExtractionService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String extractText(String fileUrl, String mimeType) throws Exception {
        byte[] bytes = downloadFile(fileUrl);
        return switch (mimeType.toLowerCase()) {
            case "application/pdf" -> extractFromPdf(bytes);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> extractFromDocx(bytes);
            case "text/plain", "text/csv" -> extractFromText(bytes);
            default -> throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
        };
    }

    private byte[] downloadFile(String fileUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download file, status: " + response.statusCode());
        }
        return response.body();
    }

    private String extractFromPdf(byte[] bytes) throws IOException {
        try (PDDocument pdf = PDDocument.load(new ByteArrayInputStream(bytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return cleanText(stripper.getText(pdf));
        }
    }

    private String extractFromDocx(byte[] bytes) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();
            docx.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return cleanText(sb.toString());
        }
    }

    private String extractFromText(byte[] bytes) {
        return cleanText(new String(bytes, StandardCharsets.UTF_8));
    }

    private String cleanText(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .replaceAll("(\r?\n){3,}", "\n\n")
                .trim();
    }
}