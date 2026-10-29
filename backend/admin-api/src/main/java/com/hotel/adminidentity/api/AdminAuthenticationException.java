package com.hotel.adminidentity.api;

public class AdminAuthenticationException extends RuntimeException {

    private final String reasonCode;

    public AdminAuthenticationException(String reasonCode) {
        super("Invalid credentials.");
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
