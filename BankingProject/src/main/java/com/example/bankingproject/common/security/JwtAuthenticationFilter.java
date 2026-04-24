package com.example.bankingproject.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor //Auto-generates constructor for dependency injection
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtservice;

    //Main method executed for every request
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        //Checks if token is missing or invalid format
        if (authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        //Extracts JWT by removing "Bearer "
        String token = authHeader.substring(7);

        try{
            String userId = jwtservice.extractUserId(token);

            //Creates authentication object with userId and no roles
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null , Collections.emptyList());

            //Attaches request-specific metadata to auth object
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );

            //Stores authenticated user in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

        }
        catch(Exception e){
            //Clears authentication if token is invalid
            SecurityContextHolder.clearContext();
        }

        //Continues request processing
        filterChain.doFilter(request,response);
    }
}

/*

Client Request
   ↓
JWT Filter runs (before controller)
   ↓
Extract Authorization header
   ↓
Check if token exists
   ↓
Extract JWT
   ↓
Validate JWT (via JwtService)
   ↓
Extract userId
   ↓
Create Authentication object
   ↓
Set in SecurityContext
   ↓
Request proceeds to controller

 */
