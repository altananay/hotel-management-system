package com.hotel.web;

import com.hotel.adminidentity.api.AdminCookies;
import com.hotel.adminidentity.api.AdminCsrfException;
import com.hotel.adminidentity.api.AdminIdentityApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AdminCsrfFilter extends OncePerRequestFilter {

    private final AdminIdentityApi identityApi;
    private final AdminProblemWriter problemWriter;

    public AdminCsrfFilter(AdminIdentityApi identityApi, AdminProblemWriter problemWriter) {
        this.identityApi = identityApi;
        this.problemWriter = problemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return !path.startsWith("/admin-api/")
                || "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method)
                || path.equals("/admin-api/auth/csrf")
                || path.startsWith("/admin-api/.well-known/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            identityApi.validateCsrf(
                    request.getHeader(AdminCookies.CSRF_HEADER),
                    cookie(request, AdminCookies.SESSION_COOKIE),
                    request.getHeader("Origin"),
                    request.getMethod(),
                    request.getRequestURI().equals("/admin-api/auth/login"));
            filterChain.doFilter(request, response);
        } catch (AdminCsrfException ex) {
            problemWriter.write(
                    request,
                    response,
                    HttpStatus.FORBIDDEN,
                    "security.csrf_invalid",
                    "CSRF validation failed.");
        }
    }

    private String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
