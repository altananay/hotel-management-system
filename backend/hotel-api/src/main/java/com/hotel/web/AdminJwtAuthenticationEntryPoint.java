package com.hotel.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AdminJwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public AdminJwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        Throwable cause = authException;
        while (cause != null) {
            if (cause instanceof ResourceAccessException
                    || cause instanceof ConnectException
                    || cause instanceof HttpTimeoutException
                    || cause instanceof UnknownHostException) {
                write(response, request, HttpStatus.SERVICE_UNAVAILABLE, "system.http_503");
                return;
            }
            cause = cause.getCause();
        }
        write(response, request, HttpStatus.UNAUTHORIZED, "security.unauthorized");
    }

    private void write(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String code)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(RequestIdFilter.HEADER_NAME, RequestIdFilter.resolveRequestId(request));
        ProblemDetail problem = ProblemDetailFactory.forStatus(status, request, code, detail(status));
        objectMapper.writeValue(response.getWriter(), problem);
    }

    private String detail(HttpStatus status) {
        return status == HttpStatus.SERVICE_UNAVAILABLE
                ? "The admin identity provider is unavailable."
                : "Authentication is required to access this resource.";
    }
}
