package com.banking.account.config;

import com.banking.account.security.InternalServiceAuthFilter;
import com.banking.common.security.CommonJwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CommonJwtAuthFilter commonJwtAuthFilter;
    private final InternalServiceAuthFilter internalServiceAuthFilter;

    public SecurityConfig(CommonJwtAuthFilter commonJwtAuthFilter,
                          InternalServiceAuthFilter internalServiceAuthFilter) {
        this.commonJwtAuthFilter = commonJwtAuthFilter;
        this.internalServiceAuthFilter = internalServiceAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/**"
                ).permitAll()
                .requestMatchers("/api/accounts/internal/**").hasAuthority("ROLE_INTERNAL_SERVICE")
                .requestMatchers(HttpMethod.PATCH, "/api/accounts/*/status").hasAnyRole("ADMIN", "SUPPORT")
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(commonJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
