package com.arcadia.common.config;

import jakarta.servlet.Filter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    /**
     * The common module deliberately does not depend on arcadia-auth (which itself
     * depends on common). The authentication module can expose its JWT filter as a
     * bean named "jwtAuthenticationFilter"; it will be inserted when present.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("jwtAuthenticationFilter") ObjectProvider<Filter> jwtFilterProvider) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated());

        jwtFilterProvider.ifAvailable(filter -> {
            try {
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not register JWT authentication filter", exception);
            }
        });

        return http.build();
    }
}
