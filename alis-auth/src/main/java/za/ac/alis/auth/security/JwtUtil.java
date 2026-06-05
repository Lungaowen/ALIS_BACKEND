package za.ac.alis.auth.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
        @Value("${alis.jwt.secret}") String secret,
        @Value("${alis.jwt.expiration:86400000}") long expirationMs
    ) {
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                "JWT secret must be at least 32 bytes. Current length: " + keyBytes.length
            );
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a JWT compatible with both Spring Security and Supabase PostgREST.
     *
     * PostgREST reads:
     *   - "role"     → maps to a PostgreSQL role (use "authenticated" for RLS)
     *   - "sub"      → the user ID
     *
     * Your app reads:
     *   - "app_role" → your business role (ADMIN, LEGAL_PRACTITIONER, etc.)
     */
    public String generateToken(String subjectId, String appRole) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(subjectId)
                .claim("role", "authenticated")   // PostgREST / RLS role
                .claim("app_role", appRole)        // your Spring Security role
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
