package com.sla.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    // Fixed secret key - in production, use environment variable
    private final String SECRET = "my-very-secret-key-for-jwt-signing-that-is-long-enough-for-hs256-algorithm";
    private final Key key;

    public JwtUtil() {
        // Create key from fixed secret
        byte[] keyBytes = Base64.getEncoder().encode(SECRET.getBytes());
        this.key = new SecretKeySpec(keyBytes, 0, 32, "HmacSHA256");
    }

    private final long expirydate=24*60*60*1000;

    public String generateToken(String email,String role){
        return Jwts.builder()
                .setSubject(email)
                .claim("role",role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+expirydate))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token){
        return parserclaims(token).getSubject();
    }

    public String extractRole(String token){
        return parserclaims(token).get("role", String.class);
    }
    public boolean validateToken(String token) {
        try {
            parserclaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Claims parserclaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
