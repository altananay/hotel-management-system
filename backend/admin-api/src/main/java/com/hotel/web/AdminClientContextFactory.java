package com.hotel.web;

import com.hotel.adminidentity.api.AdminIdentityApi.AdminClientContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Builds the single client context used by login, logout, {@code /me} and audit. The same resolver
 * runs for anonymous login requests (where {@link AdminIdentityFilter} is skipped) and for session
 * protected requests, so lockout and audit always see the same IP and request id.
 */
@Component
public class AdminClientContextFactory {

    private final ClientIpResolver clientIpResolver;

    public AdminClientContextFactory(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    public AdminClientContext resolve(HttpServletRequest request) {
        String ip = clientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String requestId = RequestIdFilter.resolveRequestId(request);
        request.setAttribute("adminClientIp", ip);
        request.setAttribute("adminUserAgent", userAgent);
        request.setAttribute("adminRequestId", requestId);
        return new AdminClientContext(ip, userAgent, requestId);
    }
}
