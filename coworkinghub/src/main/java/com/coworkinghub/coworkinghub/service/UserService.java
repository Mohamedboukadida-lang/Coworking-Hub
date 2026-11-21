package com.coworkinghub.coworkinghub.service;

import com.coworkinghub.coworkinghub.dto.AuthResponse;
import com.coworkinghub.coworkinghub.dto.LoginRequest;
import com.coworkinghub.coworkinghub.dto.PublicProfile;
import com.coworkinghub.coworkinghub.dto.RegisterRequest;
import com.coworkinghub.coworkinghub.dto.UserUpdateRequest;
import com.coworkinghub.coworkinghub.exception.ApiException;
import com.coworkinghub.coworkinghub.exception.ResourceNotFoundException;
import com.coworkinghub.coworkinghub.model.AccountType;
import com.coworkinghub.coworkinghub.model.Compte;
import com.coworkinghub.coworkinghub.repository.UserRepository;
import com.coworkinghub.coworkinghub.security.AccessService;
import com.coworkinghub.coworkinghub.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccessService accessService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       AccessService accessService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accessService = accessService;
    }

    /** Admin-only account creation (any type, including ADMIN). Hashes a password if one is supplied. */
    @Transactional
    public Compte createUser(Compte compte) {
        accessService.requireAdmin();
        compte.setId(null);
        if (compte.getEmail() != null) {
            compte.setEmail(normalizeEmail(compte.getEmail()));
            ensureEmailFree(compte.getEmail());
        }
        if (compte.getPassword() != null) {
            compte.setPassword(passwordEncoder.encode(compte.getPassword()));
        }
        return userRepository.save(compte);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        AccountType type = request.typeCompte() == null ? AccountType.USER : request.typeCompte();
        if (type == AccountType.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admin accounts cannot be self-registered");
        }
        String email = normalizeEmail(request.email());
        ensureEmailFree(email);

        Compte compte = new Compte();
        compte.setName(request.name());
        compte.setSurname(request.surname());
        compte.setAge(request.age());
        compte.setProfession(request.profession());
        compte.setEmail(email);
        compte.setPassword(passwordEncoder.encode(request.password()));
        compte.setTypeCompte(type);
        Compte saved = userRepository.save(compte);
        return new AuthResponse(jwtService.createToken(saved), saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Compte compte = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(c -> c.getPassword() != null && passwordEncoder.matches(request.password(), c.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwtService.createToken(compte), compte);
    }

    @Transactional(readOnly = true)
    public Compte getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public Compte me() {
        return accessService.caller();
    }

    /** The full account for the account itself or an admin; a {@link PublicProfile} (no email) for others. */
    @Transactional(readOnly = true)
    public Object getVisibleProfile(Long id) {
        Compte compte = getById(id);
        Compte caller = accessService.caller();
        if (AccessService.isAdmin(caller) || caller.getId().equals(compte.getId())) {
            return compte;
        }
        return PublicProfile.of(compte);
    }

    @Transactional
    public Compte update(Long id, UserUpdateRequest request) {
        Compte compte = getById(id);
        accessService.requireSelfOrAdmin(id);
        if (request.name() != null) compte.setName(request.name());
        if (request.surname() != null) compte.setSurname(request.surname());
        if (request.age() != null) compte.setAge(request.age());
        if (request.profession() != null) compte.setProfession(request.profession());
        return userRepository.save(compte);
    }

    private void ensureEmailFree(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
