package za.ac.alis.core.dto;

public class AuthResponse {

    private String message;
    private Long clientId;
    private String email;
    private String fullName;
    private String role;
    private boolean success;
    private String token;

    public AuthResponse() {}

    public static AuthResponse success(Long clientId, String email,
                                       String fullName, String role, String message) {
        AuthResponse r = new AuthResponse();
        r.success = true;
        r.message = message;
        r.clientId = clientId;
        r.email = email;
        r.fullName = fullName;
        r.role = role;
        return r;
    }

    public static AuthResponse failure(String message) {
        AuthResponse r = new AuthResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public String getMessage() {
        return message;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
