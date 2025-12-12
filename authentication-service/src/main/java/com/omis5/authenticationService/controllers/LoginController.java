package com.omis5.authenticationService.controllers;
import com.omis5.authenticationService.model.AuthenticationUser;
import com.omis5.authenticationService.config.JwtTokenGenerator;
import com.omis5.authenticationService.model.RefreshToken;
import com.omis5.authenticationService.services.AuthenticationUserService;
import com.omis5.authenticationService.services.RefreshTokenService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationUserService authenticationUserService;
    private final RefreshTokenService refreshTokenService;

    private final PasswordEncoder passwordEncoder;

    public static class LoginRequest {
        public String username;
        public String password;
    }
@Getter
@Setter
    public static class LoginResponse {
        private String token;
        private Long userId;
        private AuthenticationUser.ROLE userRole;

        public LoginResponse(String token, Long userId, AuthenticationUser.ROLE userRole) {
            this.token = token;
            this.userId = userId;
            this.userRole = userRole;
        }

    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<AuthenticationUser> authUserOpt =
                authenticationUserService.findByUsername(loginRequest.username);

        if (authUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }

        AuthenticationUser authUser = authUserOpt.get();

        if (!passwordEncoder.matches(loginRequest.password, authUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }

        RefreshToken token = refreshTokenService.generateRefreshToken(authUser);

        return ResponseEntity.ok(new LoginResponse(token.getToken(), authUser.getId(),authUser.getUserRole()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthenticationUser newUser) {

        if (authenticationUserService.existsByUsername(newUser.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username already exists");
        }
        AuthenticationUser created = authenticationUserService.createUser(newUser);
        return ResponseEntity.ok().build();
    }
}
