package com.hotel.bootstrap;

import com.hotel.adminidentity.api.AdminBootstrapApi;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapCommand;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapOutcome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapCli {

    private final AdminBootstrapApi bootstrapApi;
    private final String username;
    private final String email;
    private final String password;

    public AdminBootstrapCli(
            AdminBootstrapApi bootstrapApi,
            @Value("${ADMIN_BOOTSTRAP_USERNAME:}") String username,
            @Value("${ADMIN_BOOTSTRAP_EMAIL:}") String email,
            @Value("${ADMIN_BOOTSTRAP_PASSWORD:}") String password) {
        this.bootstrapApi = bootstrapApi;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public int run() {
        try {
            AdminBootstrapOutcome outcome =
                    bootstrapApi.bootstrap(new AdminBootstrapCommand(username, email, password));
            return outcome.exitCode();
        } catch (RuntimeException ex) {
            return 3;
        }
    }
}
