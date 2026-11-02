package com.hotel.adminidentity.api;

public final class AdminCookies {

    public static final String SESSION_COOKIE = "ADMIN_SESSION";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private AdminCookies() {}
}
