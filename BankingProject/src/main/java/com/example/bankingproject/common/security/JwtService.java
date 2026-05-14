package com.example.bankingproject.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts; //Main JWT builder/parser
import io.jsonwebtoken.security.Keys; //JWT payload (data inside token)
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service; //Generates secure cryptographic keys
import javax.crypto.SecretKey; //Represents signing key
import java.util.Base64; //Encodes key properly

import java.util.Date;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    // converts the string secret key to cryptographic key
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId){
        log.debug("Generating JWT token for userId: {}", userId);
        String token = Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) //Now + 1 hour
                .signWith(getSigningKey())// signs token using:  secret key and MAC SHA algorithm
                .compact(); //Converts JWT object → String
        log.debug("JWT token generated successfully for userId: {}", userId);
        return token;
    }

    public String extractUserId(String token) {
        try {
            String userId = extractAllClaims(token).getSubject();
            log.debug("Successfully extracted userId from JWT token");
            return userId;
        } catch (Exception e) {
            log.error("Failed to extract userId from JWT token: {}", e.getMessage());
            throw e;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)// splits the JWT and checks signature validity , expiration time, structure
                .getBody();
    }
}


/*
User logs in
   ↓
JWTService.generateToken(userId)
   ↓
JWT created (signed)
   ↓
Sent to client
 */