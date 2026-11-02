package com.hotel.adminidentity.internal.web;

import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;

public record MeResponse(String id, String username, String email, String role) {

    public static MeResponse from(AdminSessionView session) {
        return new MeResponse(
                session.userId().toString(), session.username(), session.email(), session.role());
    }
}
