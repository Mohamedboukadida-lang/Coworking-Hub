package com.coworkinghub.coworkinghub.security;

import com.coworkinghub.coworkinghub.exception.ApiException;
import com.coworkinghub.coworkinghub.model.AccountType;
import com.coworkinghub.coworkinghub.model.Compte;
import com.coworkinghub.coworkinghub.model.CoworkingSpace;
import com.coworkinghub.coworkinghub.model.Reservation;
import com.coworkinghub.coworkinghub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Who the caller is and what they may do. The account is reloaded from the database on every call, so a
 * role change or deleted account takes effect immediately rather than when the token expires.
 */
@Service
public class AccessService {

    private final UserRepository userRepository;

    public AccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The authenticated account; 401 if there is no valid token or the account no longer exists. */
    public Compte caller() {
        Long id = CurrentUser.requireId();
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
    }

    public static boolean isAdmin(Compte account) {
        return account.getTypeCompte() == AccountType.ADMIN;
    }

    public static boolean owns(Compte account, CoworkingSpace space) {
        return space.getOwner() != null && Objects.equals(space.getOwner().getId(), account.getId());
    }

    public static boolean booked(Compte account, Reservation reservation) {
        return reservation.getCompte() != null && Objects.equals(reservation.getCompte().getId(), account.getId());
    }

    public static void require(boolean allowed, String message) {
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, message);
        }
    }

    public Compte requireAdmin() {
        Compte caller = caller();
        require(isAdmin(caller), "Only administrators can do this");
        return caller;
    }

    /** Owners and admins may create spaces. */
    public Compte requireCanCreateSpace() {
        Compte caller = caller();
        require(isAdmin(caller) || caller.getTypeCompte() == AccountType.OWNER,
                "Only owner or admin accounts can create coworking spaces");
        return caller;
    }

    /** The space's owner or an admin. */
    public Compte requireCanManage(CoworkingSpace space) {
        Compte caller = caller();
        require(isAdmin(caller) || owns(caller, space), "Only the owner of this coworking space can do this");
        return caller;
    }

    /** The account itself or an admin. */
    public Compte requireSelfOrAdmin(Long accountId) {
        Compte caller = caller();
        require(isAdmin(caller) || Objects.equals(caller.getId(), accountId), "You can only change your own account");
        return caller;
    }
}
