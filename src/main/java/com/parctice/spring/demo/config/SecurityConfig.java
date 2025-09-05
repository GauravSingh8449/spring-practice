package com.parctice.spring.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()        // temporarily disable CSRF
            .authorizeHttpRequests()
            .anyRequest().permitAll();  // allow all requests without login
        return http.build();
    }
}
