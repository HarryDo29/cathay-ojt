package com.cathay.apigateway.util;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtUtil {
    @Value("${access-token.secret}")
    private String defaultSecret;

    @Value("${access-token.expire}")
    private Duration defaultExpire;

    public String buildToken(Map<String, Object> claim, String subject, Duration expire, String secret){
        long expireInMillis = defaultExpire.toMillis();
        return Jwts.builder()
                .setClaims(claim)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireInMillis))
                .signWith(getSignKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    public String buildToken(Map<String, Object> claim, String subject) {
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
}
