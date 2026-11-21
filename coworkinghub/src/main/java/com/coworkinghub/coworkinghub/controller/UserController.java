package com.coworkinghub.coworkinghub.controller;

import com.coworkinghub.coworkinghub.dto.UserUpdateRequest;
import com.coworkinghub.coworkinghub.model.Compte;
import com.coworkinghub.coworkinghub.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/me")
    public ResponseEntity<Compte> me() {
        return ResponseEntity.ok(userService.me());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getVisibleProfile(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Compte> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }
}
