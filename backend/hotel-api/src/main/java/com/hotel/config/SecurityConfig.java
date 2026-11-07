package com.hotel.config;

import com.hotel.web.AdminJwtAuthenticationEntryPoint;
import com.hotel.web.ProblemDetailFactory;
import com.hotel.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;
    private final RequestMatcher prometheusScrapeMatcher;

    public SecurityConfig(
            ObjectMapper objectMapper,
            @Value("${hotel.cors.allowed-origins:http://localhost:3000}") String allowedOriginsCsv,
            @Value("${management.prometheus.allowed-cidr:127.0.0.1/32,::1/128}")
                    String prometheusAllowedCidrCsv) {
        this.objectMapper = objectMapper;
        this.allowedOrigins =
                Arrays.stream(allowedOriginsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        List<RequestMatcher> cidrMatchers =
                Arrays.stream(prometheusAllowedCidrCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(IpAddressMatcher::new)
                        .map(matcher -> (RequestMatcher) matcher)
                        .toList();
        if (cidrMatchers.isEmpty()) {
            throw new IllegalArgumentException(
                    "management.prometheus.allowed-cidr must contain at least one CIDR");
        }
        RequestMatcher cidrMatcher =
                cidrMatchers.size() == 1
                        ? cidrMatchers.getFirst()
                        : new OrRequestMatcher(cidrMatchers);
        this.prometheusScrapeMatcher =
                new AndRequestMatcher(
                        PathPatternRequestMatcher.pathPattern("/actuator/prometheus"), cidrMatcher);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminApiSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtDecoder> jwtDecoderProvider,
            ObjectProvider<JwtAuthenticationConverter> jwtConverterProvider,
            ObjectProvider<AdminJwtAuthenticationEntryPoint> adminEntryPointProvider)
            throws Exception {
        http.securityMatcher("/admin-api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(this::writeUnauthorizedProblem)
                                        .accessDeniedHandler(this::writeForbiddenProblem));
        JwtDecoder decoder = jwtDecoderProvider.getIfAvailable();
        JwtAuthenticationConverter converter = jwtConverterProvider.getIfAvailable();
        AdminJwtAuthenticationEntryPoint entryPoint = adminEntryPointProvider.getIfAvailable();
        if (decoder != null && converter != null && entryPoint != null) {
            http.oauth2ResourceServer(
                    oauth2 ->
                            oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                                    .authenticationEntryPoint(entryPoint));
        }
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
                                        .requestMatchers(prometheusScrapeMatcher)
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
