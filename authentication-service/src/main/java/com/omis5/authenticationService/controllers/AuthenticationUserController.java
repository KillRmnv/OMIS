package com.omis5.authenticationService.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.omis5.authenticationService.model.AuthenticationUser;
import com.omis5.authenticationService.services.AuthenticationUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class AuthenticationUserController {

    private final AuthenticationUserService authenticationUserService;
    private final PasswordEncoder passwordEncoder;


    @GetMapping
    public ResponseEntity<List<AuthenticationUser>> getAllUsers() {
        List<AuthenticationUser> users = authenticationUserService.getAllUsers();
        return ResponseEntity.ok(users);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Object> getUserById(@PathVariable Long id) {
        Optional<AuthenticationUser> userOpt = authenticationUserService.findById(id);
        return userOpt.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found"));
    }



    @PostMapping
    public ResponseEntity<AuthenticationUser> createUser(@RequestBody AuthenticationUser user) {
        user.setId(null);
        AuthenticationUser createdUser = authenticationUserService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody AuthenticationUser updatedUser) {
        updatedUser.setId(null);
        Optional<AuthenticationUser> userOpt = authenticationUserService.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        AuthenticationUser user = userOpt.get();

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            authenticationUserService.updatePassword(user, updatedUser.getPassword());
        }

        user.setActive(updatedUser.isActive());

        AuthenticationUser savedUser = authenticationUserService.updateUser(user);
        return ResponseEntity.ok(savedUser);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<AuthenticationUser> userOpt = authenticationUserService.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        authenticationUserService.deactivateUser(userOpt.get());
        return ResponseEntity.noContent().build();
    }
}
