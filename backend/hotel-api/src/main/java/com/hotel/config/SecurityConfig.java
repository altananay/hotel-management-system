package com.hotel.config;

import com.hotel.web.ProblemDetailFactory;
import com.hotel.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            ObjectMapper objectMapper,
            @Value("${hotel.cors.allowed-origins:http://localhost:3000}") String allowedOriginsCsv) {
        this.objectMapper = objectMapper;
        this.allowedOrigins =
                Arrays.stream(allowedOriginsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/admin-api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(this::writeUnauthorizedProblem)
                                        .accessDeniedHandler(this::writeForbiddenProblem));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain publicApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/actuator/health")
                                        .permitAll()
                                        .requestMatchers("/actuator/**")
                                        .denyAll()
                                        .requestMatchers("/api/**")
                                        .permitAll()
                                        .requestMatchers("/error")
                                        .permitAll()
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(this::writeUnauthorizedProblem)
                                        .accessDeniedHandler(this::writeForbiddenProblem));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Content-Type", "Accept", "Origin", RequestIdFilter.HEADER_NAME));
        configuration.setExposedHeaders(List.of(RequestIdFilter.HEADER_NAME));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void writeUnauthorizedProblem(
            HttpServletRequest request, HttpServletResponse response, Exception authException)
            throws IOException {
        writeProblem(response, request, HttpStatus.UNAUTHORIZED);
    }

    private void writeForbiddenProblem(
            HttpServletRequest request, HttpServletResponse response, Exception accessDenied)
            throws IOException {
        writeProblem(response, request, HttpStatus.FORBIDDEN);
    }

    private void writeProblem(
            HttpServletResponse response, HttpServletRequest request, HttpStatus status)
            throws IOException {
        String requestId = RequestIdFilter.resolveRequestId(request);
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(RequestIdFilter.HEADER_NAME, requestId);

        ProblemDetail problem = ProblemDetailFactory.forStatus(status, request);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
