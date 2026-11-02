package com.hotel.adminidentity.api;

public class AdminSessionException extends RuntimeException {

    public AdminSessionException() {
        super("Authentication is required.");
    }
}
