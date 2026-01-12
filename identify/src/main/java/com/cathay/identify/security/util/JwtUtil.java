package com.cathay.identify.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${access-token.secret}")
    private String defaultSecret;

    @Value("${access-token.expire}")
    private Duration defaultExpire;

    public String buildToken(Map<String, String> claim, String subject, Duration expire, String secret){
        long expireInMillis = expire.toMillis();
        return Jwts.builder()
                .setClaims(claim)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireInMillis))
                .signWith(getSignKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    public String buildToken(Map<String, String> claim, String subject) {
        return buildToken(claim, subject, defaultExpire, defaultSecret);
    }

    public Claims extractToken(String token, String secret){
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Claims extractToken(String token){
        return extractToken(token, defaultSecret);
    }

    private Key getSignKey(String secret){
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public <T> T extractClaim(Claims claim, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(claim);
    }

    public String extractEmail(String token) {
        Claims claims = extractToken(token);
        return claims.getSubject();
    }

    public Date extractExpiration(String token) {
        Claims claims = extractToken(token);
        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public boolean validateToken(String token, String email) {
        try {
            String tokenEmail = extractEmail(token);
            return tokenEmail.equals(email) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
