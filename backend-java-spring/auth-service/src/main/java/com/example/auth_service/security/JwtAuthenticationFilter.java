package com.example.auth_service.security;

import com.example.auth_service.model.UserAccount;
import com.example.auth_service.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests using a Bearer JWT in the {@code Authorization} header.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Extract and parse {@code Authorization: Bearer <jwt>}.</li>
 *   <li>Validate token structure/signature/expiration via {@link JwtUtil}.</li>
 *   <li>Load the current {@link UserDetails} from the database to enforce server-side gates
 *       (for example, rejecting inactive accounts even if a token exists).</li>
 *   <li>Populate the {@link SecurityContextHolder} for downstream authorization.</li>
 * </ul>
 *
 * <p>Security notes:</p>
 * <ul>
 *   <li>This filter never logs JWTs or raw claims.</li>
 *   <li>Any malformed/invalid token results in {@code 401 Unauthorized} without detail.</li>
 *   <li>Stateless: no sessions are created; authentication is derived per request.</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // Fast-path: no Authorization header (or not Bearer) -> treat as anonymous and continue.
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Defensive: reject empty tokens early to avoid confusing downstream parsers.
        String token = header.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            // Parse + validate token. JwtUtil enforces signature and expiration.
            Claims claims = jwtUtil.parse(token);
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Only build Authentication if another filter hasn't already established one.
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load the authoritative user record so account flags can override token presence.
                UserDetails userDetails = userDetailsService.loadUserByUsername(subject);
                if (userDetails instanceof UserAccount account && !account.isActive()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);
        } catch (JwtException ex) {
            // Do not leak parsing/validation details to callers.
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
