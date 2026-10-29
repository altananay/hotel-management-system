package com.hotel.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private final List<IpAddressMatcher> trustedProxies;

    public ClientIpResolver(
            @Value("${admin.trusted-proxy-cidrs:}") String trustedProxyCidrs) {
        this.trustedProxies =
                Arrays.stream(trustedProxyCidrs.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(IpAddressMatcher::new)
                        .toList();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (trustedProxies.isEmpty() || !isTrusted(remoteAddress)) {
            return remoteAddress;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddress;
        }
        List<String> candidates = new ArrayList<>();
        for (String candidate : forwardedFor.split(",")) {
            String value = candidate.trim();
            if (!value.isEmpty()) {
                candidates.add(value);
            }
        }
        for (int index = candidates.size() - 1; index >= 0; index--) {
            String candidate = candidates.get(index);
            if (!isTrusted(candidate)) {
                return candidate;
            }
        }
        return remoteAddress;
    }

    private boolean isTrusted(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        for (IpAddressMatcher matcher : trustedProxies) {
            try {
                if (matcher.matches(address)) {
                    return true;
                }
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid forwarded address '{}'; using direct address", address);
                return false;
            }
        }
        return false;
    }
}
