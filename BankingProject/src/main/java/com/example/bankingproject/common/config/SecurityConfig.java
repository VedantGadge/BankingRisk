package com.example.bankingproject.common.config;

import com.example.bankingproject.user.dto.RegisterRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration // tells spring that there this a bean defined and register it
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}

/*

Spring will:
1. Start application
2. See @Configuration
3. Run passwordEncoder()
4. Store BCryptPasswordEncoder in container
5. Inject wherever needed

 */
