package com.example.bankingproject.common.config;

import com.example.bankingproject.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@RequiredArgsConstructor
@Configuration // tells spring that there this a bean defined and register it
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf-> csrf.disable()) // Disables CSRF protection (not needed for stateless JWT APIs)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Allows unauthenticated access to auth endpoints (login/register)
                        .anyRequest().authenticated() // Requires authentication for all other endpoints
                )

                .sessionManagement(session->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        // Ensures no HTTP session is created (JWT-based auth)
                )

                // Adds JWT filter before Spring’s default authentication filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build(); // Builds and returns the configured security filter chain

    }
}

/*

Spring Security Flow:

1. Application starts
2. Spring scans for @Configuration classes
3. Detects SecurityConfig
4. Registers PasswordEncoder bean (BCrypt)
5. Builds SecurityFilterChain using securityFilterChain()

6. Configures HTTP security:
   - Disables CSRF (stateless APIs)
   - Allows /api/auth/** without authentication
   - Requires authentication for all other endpoints

7. Sets session policy to STATELESS
   - No HTTP sessions used
   - Every request must carry JWT

8. Registers JwtAuthenticationFilter in filter chain
   - Runs BEFORE UsernamePasswordAuthenticationFilter

9. For every incoming request:
   - JwtAuthenticationFilter extracts token from header
   - Validates token using JwtService
   - Sets Authentication in SecurityContext if valid

10. Controller executes ONLY if request is authenticated

*/
