package com.hotel.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);
    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");

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
            if (!isIpLiteral(candidate)) {
                log.warn(
                        "Invalid X-Forwarded-For address '{}'; using direct address",
                        candidate);
                return remoteAddress;
            }
            if (!isTrusted(candidate)) {
                return candidate;
            }
        }
        return remoteAddress;
    }

    private boolean isIpLiteral(String value) {
        if (value == null
                || value.isBlank()
                || (!value.contains(".") && !value.contains(":"))
                || !IP_LITERAL.matcher(value).matches()) {
            return false;
        }
        try {
            new IpAddressMatcher(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
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
                return false;
            }
        }
        return false;
    }
}
