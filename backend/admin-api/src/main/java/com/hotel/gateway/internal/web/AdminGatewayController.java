package com.hotel.gateway.internal.web;

import com.hotel.gateway.internal.application.AdminGatewayClient;
import com.hotel.gateway.internal.application.GatewayResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminGatewayController {

    private final AdminGatewayClient gatewayClient;

    public AdminGatewayController(AdminGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @RequestMapping("/admin-api/**")
    public ResponseEntity<byte[]> forward(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body,
            @RequestAttribute(name = "adminJwt") String jwt) {
        GatewayResponse response = gatewayClient.exchange(request, body, jwt);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(response.contentType()));
        headers.set("X-Request-Id", response.requestId());
        return ResponseEntity.status(response.status()).headers(headers).body(response.body());
    }
}
