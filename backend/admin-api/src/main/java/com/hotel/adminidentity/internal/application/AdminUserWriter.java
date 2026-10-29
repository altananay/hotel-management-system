package com.hotel.adminidentity.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.adminidentity.internal.domain.AdminRole;
import com.hotel.adminidentity.internal.domain.Argon2PasswordHasher;
import com.hotel.adminidentity.internal.persistence.AdminUserEntity;
import com.hotel.adminidentity.internal.persistence.AdminUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserWriter {

    private final AdminUserRepository userRepository;
    private final Argon2PasswordHasher passwordHasher;
    private final Clock clock;

    public AdminUserWriter(
            AdminUserRepository userRepository, Argon2PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdminUserEntity createSuperAdmin(String username, String email, String password) {
        Instant now = clock.instant();
        return userRepository.save(
                new AdminUserEntity(
                        Generators.timeBasedEpochGenerator().generate(),
                        username.trim().toLowerCase(Locale.ROOT),
                        email.trim().toLowerCase(Locale.ROOT),
                        passwordHasher.hash(password),
                        AdminRole.SUPER_ADMIN,
                        true,
                        now,
                        now));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(UUID id) {
        userRepository.deleteById(id);
    }
}
