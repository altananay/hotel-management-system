package com.hotel.adminidentity.internal.web;

import com.hotel.adminidentity.internal.application.AdminJwtKeyProvider;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminJwksController {

    private final AdminJwtKeyProvider keyProvider;

    public AdminJwksController(AdminJwtKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/admin-api/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return keyProvider.publicJwks().toJSONObject();
    }
}
