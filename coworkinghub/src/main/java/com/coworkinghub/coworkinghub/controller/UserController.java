package com.coworkinghub.coworkinghub.controller;

import com.coworkinghub.coworkinghub.model.Compte;
import com.coworkinghub.coworkinghub.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<Compte> createUser(@RequestBody Compte compte) {
        return ResponseEntity.ok(userService.createUser(compte));
    }
}
