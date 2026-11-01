package com.hotel.web;

import com.hotel.adminidentity.api.AdminCookies;
import com.hotel.adminidentity.api.AdminIdentityApi;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminClientContext;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.hotel.adminidentity.api.AdminSessionException;
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
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AdminIdentityFilter extends OncePerRequestFilter {

    private final AdminIdentityApi identityApi;
    private final AdminProblemWriter problemWriter;
    private final AdminClientContextFactory clientContextFactory;

    public AdminIdentityFilter(
            AdminIdentityApi identityApi,
            AdminProblemWriter problemWriter,
            AdminClientContextFactory clientContextFactory) {
        this.identityApi = identityApi;
        this.problemWriter = problemWriter;
        this.clientContextFactory = clientContextFactory;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/admin-api/")
                || path.equals("/admin-api/auth/login")
                || path.equals("/admin-api/auth/csrf")
                || path.startsWith("/admin-api/.well-known/")
                // Test-only probe controllers live in test sources and are not exposed in production.
                || path.startsWith("/admin-api/test/")
                || path.equals("/actuator/health")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AdminClientContext clientContext = clientContext(request);
        String rawToken = cookie(request, AdminCookies.SESSION_COOKIE);
        try {
            AdminSessionView session =
                    identityApi.validateSession(rawToken, clientContext);
            request.setAttribute("adminSession", session);
            if (!request.getRequestURI().startsWith("/admin-api/auth/")) {
                request.setAttribute("adminJwt", identityApi.issueJwt(session));
            }
            filterChain.doFilter(request, response);
        } catch (AdminSessionException ex) {
            problemWriter.write(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "security.unauthorized",
                    "Authentication is required to access this resource.");
        }
    }

    private AdminClientContext clientContext(HttpServletRequest request) {
        Object attribute = request.getAttribute(AdminIdentityApi.CLIENT_CONTEXT_ATTRIBUTE);
        if (attribute instanceof AdminClientContext context) {
            return context;
        }
        return clientContextFactory.resolve(request);
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
