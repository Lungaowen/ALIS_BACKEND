package za.ac.alis.core.dto;

import java.time.LocalDateTime;

// ─────────────────────────────────────────────
// ADMIN DTOs
// ─────────────────────────────────────────────

public class AdminDTO {

    // ── Request (create) ──────────────────────────────────────────────────────
    public static class Request {
        private String fullName;
        private String email;
        private String password; // plain-text; hashed in service layer

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // ── Response (read) ───────────────────────────────────────────────────────
    public static class Response {
        private Long adminId;
        private String fullName;
        private String email;
        private LocalDateTime createdAt;

        public Long getAdminId() { return adminId; }
        public void setAdminId(Long adminId) { this.adminId = adminId; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    // ── Summary (lightweight, used inside AuditLog responses) ─────────────────
    public static class Summary {
        private Long adminId;
        private String fullName;
        private String email;

        public Long getAdminId() { return adminId; }
        public void setAdminId(Long adminId) { this.adminId = adminId; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
