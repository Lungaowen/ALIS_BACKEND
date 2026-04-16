package za.ac.alis.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"Client\"")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String passwordHash;

    private String role = "user";

    private LocalDateTime createdAt = LocalDateTime.now();

    // getters and setters

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
