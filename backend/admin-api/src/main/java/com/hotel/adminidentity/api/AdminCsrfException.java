package com.hotel.adminidentity.api;

public class AdminCsrfException extends RuntimeException {

    public AdminCsrfException() {
        super("CSRF validation failed.");
    }
}
