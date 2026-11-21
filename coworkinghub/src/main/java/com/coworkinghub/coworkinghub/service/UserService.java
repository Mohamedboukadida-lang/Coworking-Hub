package com.coworkinghub.coworkinghub.service;

import com.coworkinghub.coworkinghub.model.Compte;
import com.coworkinghub.coworkinghub.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Compte createUser(Compte compte) {
        return userRepository.save(compte);
    }
}
