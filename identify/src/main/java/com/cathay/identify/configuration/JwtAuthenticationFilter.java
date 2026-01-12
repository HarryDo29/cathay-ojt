package com.cathay.identify.configuration;

import com.cathay.identify.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // If authenticated by InternalApiKeyFilter, skip JWT verification
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Take token from header
        String authHeader = request.getHeader("Authorization");

        // If not contain in header or not start with 'Bearer ', skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Take token (Remove "Bearer " prefix)
            String token = authHeader.substring(7);

            // Parse and verify token
            Claims claims = jwtUtil.extractToken(token);

            // take user info from claims
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            // Create authorities from role
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );
            // Create authentication token
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    authorities
            );
            // Set additional details for authentication
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // Push authentication into SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (ExpiredJwtException e) {
            //response as jwt expired
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                  "error": "UNAUTHORIZED",
                  "code": "JWT_EXPIRED",
                  "message": "JWT token has expired"
                }
                """);
            return;
        } catch (JwtException e) {
            // response as jwt invalid
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                  "error": "UNAUTHORIZED",
                  "code": "JWT_INVALID",
                  "message": "Invalid JWT token"
                }
                """);
            return;
        }
        // continue filter chain
        filterChain.doFilter(request, response);
    }
}

