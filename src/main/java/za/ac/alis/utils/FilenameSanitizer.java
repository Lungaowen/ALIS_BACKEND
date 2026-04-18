package za.ac.alis.utils;

/**
 * SECURITY LAYER — Filename Sanitizer
 *
 * Prevents:
 *  - Path traversal attacks  (../../etc/passwd)
 *  - Null bytes              (%00)
 *  - Reserved OS names       (CON, PRN, AUX on Windows)
 *  - Dangerous extensions    (.exe, .sh, .php, etc.)
 *  - Overly long filenames   (filesystem limits)
 */
public class FilenameSanitizer {

    // ── Allowed MIME types for this legal document system ─────────────────────
    private static final java.util.Set<String> ALLOWED_MIME_TYPES = java.util.Set.of(
        "application/pdf",
        "text/plain",
        "text/csv",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // ── Blocked file extensions ────────────────────────────────────────────────
    private static final java.util.Set<String> BLOCKED_EXTENSIONS = java.util.Set.of(
        "exe", "sh", "bat", "cmd", "php", "js", "py",
        "rb", "pl", "jar", "war", "class", "html", "htm"
    );

    // ── Windows reserved names ─────────────────────────────────────────────────
    private static final java.util.Set<String> RESERVED_NAMES = java.util.Set.of(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4",
        "LPT1", "LPT2", "LPT3"
    );

    private static final int MAX_FILENAME_LENGTH = 200;

    /**
     * Sanitizes a raw filename from a multipart upload.
     * Returns a safe, clean filename string.
     *
     * @throws IllegalArgumentException if file is unsafe or unsupported
     */
    public static String sanitize(String rawFilename, String mimeType) {
        if (rawFilename == null || rawFilename.isBlank()) {
            throw new IllegalArgumentException("Filename is missing or empty.");
        }

        // ── 1. Strip null bytes ────────────────────────────────────────────────
        String name = rawFilename.replace("\0", "");

        // ── 2. Strip path separators (path traversal prevention) ──────────────
        //       e.g. "../../etc/passwd" → "passwd"
        name = java.nio.file.Paths.get(name).getFileName().toString();

        // ── 3. Strip leading dots and spaces ──────────────────────────────────
        name = name.replaceAll("^[.\\s]+", "");

        // ── 4. Replace any remaining unsafe characters ─────────────────────────
        //       Keep only alphanumeric, dash, underscore, dot
        name = name.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        // ── 5. Enforce max length ──────────────────────────────────────────────
        if (name.length() > MAX_FILENAME_LENGTH) {
            String ext = getExtension(name);
            name = name.substring(0, MAX_FILENAME_LENGTH - ext.length() - 1) + "." + ext;
        }

        // ── 6. Check for empty name after sanitization ─────────────────────────
        if (name.isBlank() || name.equals(".")) {
            throw new IllegalArgumentException("Filename is invalid after sanitization.");
        }

        // ── 7. Block reserved OS names ─────────────────────────────────────────
        String nameWithoutExt = name.contains(".")
                ? name.substring(0, name.lastIndexOf('.')).toUpperCase()
                : name.toUpperCase();
        if (RESERVED_NAMES.contains(nameWithoutExt)) {
            throw new IllegalArgumentException("Filename uses a reserved OS name: " + nameWithoutExt);
        }

        // ── 8. Block dangerous extensions ─────────────────────────────────────
        String ext = getExtension(name).toLowerCase();
        if (BLOCKED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("File extension not allowed: ." + ext);
        }

        // ── 9. Validate MIME type against allowlist ────────────────────────────
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType
                + ". Allowed: PDF, TXT, CSV, DOC, DOCX.");
        }

        return name;
    }

    /** Extracts file extension, returns empty string if none. */
    public static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1)
                ? filename.substring(dot + 1)
                : "";
    }
}
