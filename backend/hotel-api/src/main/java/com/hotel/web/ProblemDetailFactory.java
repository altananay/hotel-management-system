package com.hotel.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemDetailFactory {

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    private ProblemDetailFactory() {}

    public static ProblemDetail create(
            HttpStatus status,
            String title,
            String detail,
            String code,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(ABOUT_BLANK);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("requestId", RequestIdFilter.resolveRequestId(request));
        return problem;
    }

    /** Statusa göre standart kod, başlık ve genel mesajla problem üretir (docs/conventions.md §2.3). */
    public static ProblemDetail forStatus(HttpStatus status, HttpServletRequest request) {
        return forStatus(status, request, codeFor(status), defaultDetailFor(status));
    }

    public static ProblemDetail forStatus(
            HttpStatus status, HttpServletRequest request, String code, String detail) {
        return create(status, status.getReasonPhrase(), detail, code, request);
    }

    public static String codeFor(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "validation.invalid_request";
            case 401 -> "security.unauthorized";
            case 403 -> "security.forbidden";
            case 404 -> "system.not_found";
            case 405 -> "system.method_not_allowed";
            case 406 -> "system.not_acceptable";
            case 409 -> "system.conflict";
            case 415 -> "system.unsupported_media_type";
            default ->
                    status.is5xxServerError()
                            ? "system.internal_error"
                            : "system.http_" + status.value();
        };
    }

    public static String defaultDetailFor(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "Request validation failed.";
            case 401 -> "Authentication is required to access this resource.";
            case 403 -> "You do not have permission to access this resource.";
            case 404 -> "The requested resource was not found.";
            case 405 -> "The HTTP method is not supported for this resource.";
            case 406 -> "The requested response format is not supported.";
            case 409 -> "The request conflicts with the current state of the resource.";
            case 415 -> "The request content type is not supported.";
            default ->
                    status.is5xxServerError()
                            ? "An unexpected internal error occurred."
                            : "The request could not be processed.";
        };
    }
}
