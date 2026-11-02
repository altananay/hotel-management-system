package com.hotel.gateway.api;

public class GatewayTimeoutException extends RuntimeException {

    public GatewayTimeoutException() {
        super("Gateway upstream timed out.");
    }
}
