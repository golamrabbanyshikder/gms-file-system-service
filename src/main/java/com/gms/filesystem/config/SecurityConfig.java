package com.gms.filesystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * file-system-service is an internal service sitting behind gateway-service,
 * but is also reachable directly by external medical-partner integrations -
 * access control for that is handled by ApiKeyAuthFilter (X-API-Key header),
 * not Spring Security's own auth mechanisms, which stay fully disabled here.
 * Without this config at all, spring-boot-starter-security's default
 * auto-configuration HTTP-Basic-protects every endpoint with a random
 * per-boot password, which would 401 every call before ApiKeyAuthFilter
 * even runs.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private ApiKeyAuthFilter apiKeyAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
