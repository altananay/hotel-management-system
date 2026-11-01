package com.hotel.adminidentity.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 254) String identifier,
        @NotBlank @Size(max = 512) String password) {}
