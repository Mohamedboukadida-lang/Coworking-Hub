package com.coworkinghub.coworkinghub.repository;

import com.coworkinghub.coworkinghub.model.Compte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Compte, Long> {
}
