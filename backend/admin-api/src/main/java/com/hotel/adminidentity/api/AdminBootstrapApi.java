package com.hotel.adminidentity.api;

import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapCommand;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminBootstrapOutcome;

/**
 * Narrow bootstrap port used by the CLI. Keeping this separate from {@link AdminIdentityApi}
 * lets the bootstrap command run without web/JWT/CSRF beans.
 */
public interface AdminBootstrapApi {

    AdminBootstrapOutcome bootstrap(AdminBootstrapCommand command);
}
