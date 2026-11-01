package com.hotel.adminidentity.internal.domain;

import java.util.UUID;

public record AdminUser(
        UUID id,
        String username,
        String email,
        String passwordHash,
        AdminRole role,
        boolean active) {}
