package com.coworkinghub.coworkinghub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying "Authorization: Bearer &lt;jwt&gt;". Requests without the header pass
 * through anonymously; a header with an invalid or expired token is rejected with 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }

        Claims claims;
        Long userId;
        try {
            claims = jwtService.parse(header.substring(BEARER.length()).trim());
            userId = Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            SecurityResponses.writeUnauthorized(response, "Invalid or expired token");
            return;
        }

        String type = claims.get("type", String.class);
        List<SimpleGrantedAuthority> authorities = type == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + type));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, authorities));
        chain.doFilter(request, response);
    }
}
