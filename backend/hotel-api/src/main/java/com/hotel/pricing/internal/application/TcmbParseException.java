package com.hotel.pricing.internal.application;

public class TcmbParseException extends RuntimeException {

    public TcmbParseException(String message) {
        super(message);
    }

    public TcmbParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
