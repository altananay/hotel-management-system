package com.hotel.web;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    public static final String REQUEST_ATTRIBUTE = "requestId";

    private static final Pattern VALID_REQUEST_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");
    private static final TimeBasedEpochGenerator UUID_V7_GENERATOR =
            Generators.timeBasedEpochGenerator();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER_NAME);
        String requestId =
                (incoming != null && VALID_REQUEST_ID.matcher(incoming).matches())
                        ? incoming
                        : UUID_V7_GENERATOR.generate().toString();

        request.setAttribute(REQUEST_ATTRIBUTE, requestId);
        response.setHeader(HEADER_NAME, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    public static String resolveRequestId(HttpServletRequest request) {
        Object attr = request.getAttribute(REQUEST_ATTRIBUTE);
        if (attr instanceof String id && !id.isBlank()) {
            return id;
        }
        String mdc = MDC.get(MDC_KEY);
        if (mdc != null && !mdc.isBlank()) {
            return mdc;
        }
        return UUID_V7_GENERATOR.generate().toString();
    }
}
