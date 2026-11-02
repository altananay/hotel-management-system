package com.hotel.adminidentity.internal.domain;

public class InvalidIdentityException extends RuntimeException {

    public InvalidIdentityException(String message) {
        super(message);
    }
}
