package com.eav02.backend.common.security;

import java.time.Clock;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.eav02.backend.auth.service.TokenService;

@Configuration
public class SecurityConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    JwtDecoder jwtDecoder(TokenService tokens) {
        return tokens.decoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://eav-02-fabrica-escuela.vercel.app"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder,
            SessionJwtAuthenticationConverter sessionConverter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(decoder)
                        .jwtAuthenticationConverter(sessionConverter)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/{id}/comments", "/api/v1/posts/{id}/comments/count", "/api/v1/posts/{id}/likes",
                                "/api/v1/projects/{id}/comments", "/api/v1/projects/{id}/likes",
                                "/api/v1/comments/{id}/replies").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/posts/{id}/comments", "/api/v1/projects/{id}/comments",
                                "/api/v1/comments/{id}/replies").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/posts/{id}/likes/me", "/api/v1/projects/{id}/likes/me").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/{id}/likes/me", "/api/v1/projects/{id}/likes/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/posts").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/{postId}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/posts/{postId}/resolve").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/{postId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/projects/**").authenticated()

                        .requestMatchers("/api/v1/profile/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/profiles/**").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/posts/{id}/reports", "/api/v1/comments/{id}/reports",
                                "/api/v1/profiles/{id}/reports").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/{id}/reports/me", "/api/v1/comments/{id}/reports/me",
                                "/api/v1/profiles/{id}/reports/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{userId}/follow-summary",
                                "/api/v1/users/{userId}/followers", "/api/v1/users/{userId}/following").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/{userId}/followers/me").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{userId}/followers/me").authenticated()
                        .anyRequest().denyAll())
                .build();
    }
}
