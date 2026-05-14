package com.poker.config;

import com.poker.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/",
            "/ws/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isWhiteListed(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token != null) {
            try {
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserId(token);
                    String username = jwtUtil.getUsername(token);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    username,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT auth success | userId={} | path={}", userId, path);
                } else {
                    log.debug("JWT token invalid | path={}", path);
                }
            } catch (Exception e) {
                log.debug("JWT parse failed: {} | path={}", e.getMessage(), path);
            }
        } else {
            log.debug("No Authorization header | path={}", path);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}