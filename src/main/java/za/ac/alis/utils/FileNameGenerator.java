package za.ac.alis.utils;

import java.util.UUID;

public class FileNameGenerator {

    public static String generate(String originalFilename) {

        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "file";
        }

        String extension = extractExtension(originalFilename);

        return "doc_" +
                UUID.randomUUID().toString().replace("-", "") +
                "_" +
                System.currentTimeMillis() +
                extension;
    }

    public static String generateWithDocumentId(Long documentId, String originalFilename) {

        String extension = extractExtension(originalFilename);

        return "doc_" +
                documentId +
                "_" +
                UUID.randomUUID().toString().substring(0, 8) +
                "_" +
                System.currentTimeMillis() +
                extension;
    }

    private static String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');

        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return ""; // no extension
        }

        return filename.substring(lastDot).toLowerCase();
    }
}