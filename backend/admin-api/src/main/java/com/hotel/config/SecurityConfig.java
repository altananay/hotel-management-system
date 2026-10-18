package com.hotel.config;

import com.hotel.web.ProblemDetailFactory;
import com.hotel.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/actuator/health")
                                        .permitAll()
                                        .requestMatchers("/actuator/**")
                                        .denyAll()
                                        .requestMatchers("/admin-api/system/sample-items")
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
