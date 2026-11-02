package com.hotel.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminJwtTestController {

    @GetMapping("/admin-api/test/authenticated")
    public String authenticated() {
        return "authenticated";
    }

    @GetMapping("/admin-api/test/manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    public String manager() {
        return "manager";
    }

    @GetMapping("/admin-api/test/super-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String superAdmin() {
        return "super-admin";
    }
}
