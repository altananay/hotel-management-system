package com.hotel.gateway.internal.application;

public record GatewayResponse(int status, String contentType, byte[] body, String requestId) {}
