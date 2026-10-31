package com.hotel.adminidentity.api;

public class AdminAuditException extends RuntimeException {

    public AdminAuditException(Throwable cause) {
        super("Audit write failed.", cause);
    }
}
