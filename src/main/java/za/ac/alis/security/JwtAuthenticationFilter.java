package za.ac.alis.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import za.ac.alis.entities.Admin;
import za.ac.alis.entities.Client;
import za.ac.alis.repo.AdminRepository;
import za.ac.alis.repo.ClientRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final ClientRepository clientRepository;
    private final AdminRepository adminRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   ClientRepository clientRepository,
                                   AdminRepository adminRepository) {
        this.jwtUtil = jwtUtil;
        this.clientRepository = clientRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        log.debug("Authorization header: {}", header);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            log.debug("Extracted token: {}", token);

            if (jwtUtil.validateToken(token)) {
                log.debug("Token is valid");
                Claims claims = jwtUtil.parseClaims(token);
                String userId = claims.getSubject();
                // Read the correct claim – "user_role"
                String role = claims.get("user_role", String.class);
                String email = claims.get("email", String.class);
                log.debug("Token claims: userId={}, user_role={}, email={}", userId, role, email);

                if (isClientRole(role)) {
                    log.debug("Client role detected, checking active status for userId={}", userId);
                    if (!isActiveClient(userId)) {
                        log.warn("Client is inactive: userId={}", userId);
                        filterChain.doFilter(request, response);
                        return;
                    }
                    // Set authentication with the correct role
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Authentication set for userId={} with authorities={}", userId, authorities);
                } else {
                    // Admin role handling
                    log.debug("Admin role detected, checking admin existence");
                    try {
                        Optional<Admin> adminOpt = adminRepository.findById(Long.valueOf(userId));
                        if (adminOpt.isPresent()) {
                            List<SimpleGrantedAuthority> authorities = List.of(
                                    new SimpleGrantedAuthority("ROLE_ADMIN")
                            );
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            log.debug("Admin authentication set for userId={}", userId);
                        } else {
                            log.warn("Admin not found for userId={}", userId);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid admin ID format: {}", userId);
                    }
                }
            } else {
                log.debug("Token validation failed");
            }
        } else {
            log.debug("No Bearer token found in Authorization header");
        }
        filterChain.doFilter(request, response);
    }

    private boolean isClientRole(String role) {
        return "USER".equals(role)
                || "LEGAL_PRACTITIONER".equals(role)
                || "DEAL_MAKER".equals(role);
    }

    private boolean isActiveClient(String userId) {
        try {
            return clientRepository.findById(Long.valueOf(userId))
                    .map(Client::isActive)
                    .orElse(false);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}