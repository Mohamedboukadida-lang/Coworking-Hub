package com.coworkinghub.coworkinghub.security;

import com.coworkinghub.coworkinghub.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** The account id of the caller, as set by {@link JwtAuthenticationFilter}. Prefer {@link AccessService#caller()}. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<Long> id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            return Optional.of(id);
        }
        return Optional.empty();
    }

    public static Long requireId() {
        return id().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
