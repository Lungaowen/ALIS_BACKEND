package za.ac.alis.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import za.ac.alis.core.port.ActiveClientChecker;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ActiveClientChecker activeClientChecker;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ActiveClientChecker activeClientChecker) {
        this.jwtUtil = jwtUtil;
        this.activeClientChecker = activeClientChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.parseClaims(token);
                String userId = claims.getSubject();

                // Read the actual business role from "app_role"
                String role = claims.get("app_role", String.class);
                // Fallback to "role" claim for backward compatibility
                if (role == null) {
                    role = claims.get("role", String.class);
                }

                if (isClientRole(role) && !isActiveClient(userId)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isClientRole(String role) {
        return "USER".equals(role)
                || "LEGAL_PRACTITIONER".equals(role)
                || "DEAL_MAKER".equals(role);
    }

    private boolean isActiveClient(String userId) {
        return activeClientChecker.isActiveClient(userId);
    }
}