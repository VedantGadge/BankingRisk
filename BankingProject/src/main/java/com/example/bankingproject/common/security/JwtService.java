package com.example.bankingproject.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts; //Main JWT builder/parser
import io.jsonwebtoken.security.Keys; //JWT payload (data inside token)
import org.springframework.stereotype.Service; //Generates secure cryptographic keys
import javax.crypto.SecretKey; //Represents signing key
import java.util.Base64; //Encodes key properly

import java.util.Date;

@Service
public class JwtService {

    private final String SECRET = "my-super-secret-key-vedant-gadge"; //signing secret

    // converts teh string secret key to cryptographic key
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getEncoder().encode(SECRET.getBytes());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId){
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) //Now + 1 hour
                .signWith(getSigningKey())// signs token using:  secret key and MAC SHA algorithm
                .compact(); //Converts JWT object → String
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
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