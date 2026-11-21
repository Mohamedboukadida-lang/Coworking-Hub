package com.coworkinghub.coworkinghub.security;

import com.coworkinghub.coworkinghub.model.AccountType;
import com.coworkinghub.coworkinghub.model.Compte;
import com.coworkinghub.coworkinghub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator from APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD if no account with that email
 * exists. Admin accounts cannot be self-registered, so this is how the first one comes to exist.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            return;
        }
        Compte admin = new Compte();
        admin.setName("Administrator");
        admin.setEmail(normalized);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setTypeCompte(AccountType.ADMIN);
        userRepository.save(admin);
        log.info("Created administrator account {}", normalized);
    }
}
