package com.coworkinghub.coworkinghub.repository;

import com.coworkinghub.coworkinghub.model.Compte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Compte, Long> {
    Optional<Compte> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
