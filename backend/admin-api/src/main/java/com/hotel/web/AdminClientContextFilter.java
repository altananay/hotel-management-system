package com.hotel.web;

import com.hotel.adminidentity.api.AdminIdentityApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the client context before any admin filter runs, including anonymous login and CSRF
 * requests. The context is stored as a request attribute and reused by the identity filter and the
 * auth controller.
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class AdminClientContextFilter extends OncePerRequestFilter {

    private final AdminClientContextFactory clientContextFactory;

    public AdminClientContextFilter(AdminClientContextFactory clientContextFactory) {
        this.clientContextFactory = clientContextFactory;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/admin-api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        request.setAttribute(
                AdminIdentityApi.CLIENT_CONTEXT_ATTRIBUTE,
                clientContextFactory.resolve(request));
        filterChain.doFilter(request, response);
    }
}
