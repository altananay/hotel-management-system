package com.hotel.gateway.api;

public class GatewayUnavailableException extends RuntimeException {

    public GatewayUnavailableException(Throwable cause) {
        super("Gateway upstream is unavailable.", cause);
    }
}
