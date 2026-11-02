package com.hotel.web;

import com.hotel.adminidentity.api.AdminAuditException;
import com.hotel.adminidentity.api.AdminAuthenticationException;
import com.hotel.adminidentity.api.AdminSessionException;
import com.hotel.gateway.api.GatewayTimeoutException;
import com.hotel.gateway.api.GatewayUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<ProblemDetail> handleValidationException(
            Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(
            Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetail> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_ACCEPTABLE, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(AdminAuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAdminAuthentication(
            AdminAuthenticationException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, request, "security.invalid_credentials");
    }

    @ExceptionHandler(AdminSessionException.class)
    public ResponseEntity<ProblemDetail> handleAdminSession(
            AdminSessionException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, request, "security.unauthorized");
    }

    @ExceptionHandler(AdminAuditException.class)
    public ResponseEntity<ProblemDetail> handleAdminAudit(
            AdminAuditException ex, HttpServletRequest request) {
        log.error("Admin audit write failed for request {}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, request, "system.internal_error");
    }

    @ExceptionHandler(GatewayTimeoutException.class)
    public ResponseEntity<ProblemDetail> handleGatewayTimeout(
            GatewayTimeoutException ex, HttpServletRequest request) {
        return problem(
                HttpStatus.GATEWAY_TIMEOUT,
                request,
                "system.gateway_timeout",
                "The admin upstream timed out.");
    }

    @ExceptionHandler(GatewayUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleGatewayUnavailable(
            GatewayUnavailableException ex, HttpServletRequest request) {
        log.warn("Admin gateway upstream is unavailable: {}", ex.getMessage());
        return problem(
                HttpStatus.BAD_GATEWAY,
                request,
                "system.gateway_unavailable",
                "The admin upstream is unavailable.");
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleErrorResponseException(
            ErrorResponseException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return problem(status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for request {}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetailFactory.forStatus(status, request);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, HttpServletRequest request, String code) {
        return problem(status, request, code, ProblemDetailFactory.defaultDetailFor(status));
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, HttpServletRequest request, String code, String detail) {
        ProblemDetail problem =
                ProblemDetailFactory.forStatus(status, request, code, detail);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
