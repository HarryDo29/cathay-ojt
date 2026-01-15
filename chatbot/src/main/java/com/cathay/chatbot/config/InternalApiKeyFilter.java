//package com.cathay.chatbot.config;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.List;
//
//@Slf4j
//@Component
//public class InternalApiKeyFilter extends OncePerRequestFilter {
//
//    @Value("${internal.api.key}")
//    private String internalApiKey;
//
//    // List of public endpoints which not verify
//    private static final List<String> PUBLIC_ENDPOINTS = List.of(
//            "/auth/login",
//            "/auth/register",
//            "/login/oauth2/code/google",  // Spring Security OAuth2 callback
//            "/oauth2/authorization/google"  // OAuth2 authorization
//    );
//
//    @Override
//    protected void doFilterInternal(
//            HttpServletRequest req,
//            HttpServletResponse res,
//            FilterChain filterChain
//    ) throws ServletException, IOException {
//        String path = req.getRequestURI();
//
//        // Check API key FIRST
//        // Take internal API key from header (all endpoint go from apigateway must have API key)
//        String requestApiKey = req.getHeader("X-Internal-API-Key");
//
//        if (requestApiKey == null || requestApiKey.isEmpty()) {
//            log.error("❌ FORBIDDEN: Direct access to {} from IP: {} - Missing Internal API Key",
//                    path, req.getRemoteAddr());
//            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            res.setContentType("application/json");
//            res.getWriter().write("""
//                {
//                  "status": 403,
//                  "error": "FORBIDDEN",
//                  "code": "MISSING_INTERNAL_API_KEY",
//                  "message": "Direct access is forbidden. All requests must go through the API Gateway.",
//                  "path": "%s"
//                }
//                """.formatted(path));
//            return;
//        }
//
//        // Verify API key
//        if (!internalApiKey.equals(requestApiKey)) {
//            log.error("❌ FORBIDDEN: Invalid API Key from IP: {}", req.getRemoteAddr());
//            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            res.setContentType("application/json");
//            res.getWriter().write("""
//                {
//                  "status": 403,
//                  "error": "FORBIDDEN",
//                  "code": "INVALID_INTERNAL_API_KEY",
//                  "message": "The provided internal API key is invalid.",
//                  "path": "%s"
//                }
//                """.formatted(path));
//            return;
//        }
//
//        // Check public endpoint
//        boolean isPublicEndpoint = PUBLIC_ENDPOINTS.stream()
//                .anyMatch(path::startsWith);
//
//        if (isPublicEndpoint) {
//            // Public endpoint do not have authentication, go through
//            filterChain.doFilter(req, res);
//            return;
//        }
//
//        // Protected endpoint with valid API key
//        String email = req.getHeader("X-User-Email");
//        String role = req.getHeader("X-User-Role");
//        String userId = req.getHeader("X-User-Id");
//
//        log.info("🔐 Protected endpoint - User: {}, Role: {}, ID: {}", email, role, userId);
//
//        if (email != null && !email.isEmpty() &&
//                SecurityContextHolder.getContext().getAuthentication() == null) {
//            // Create authorities from role
//            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
//                    new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER"))
//            );
//            // Create authentication token
//            UsernamePasswordAuthenticationToken authToken =
//                    new UsernamePasswordAuthenticationToken(email, null, authorities);
//            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
//            // Push into SecurityContext
//            SecurityContextHolder.getContext().setAuthentication(authToken);
//        }
//
//        // Continue filter chain
//        filterChain.doFilter(req, res);
//    }
//}
//
