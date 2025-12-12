package com.omis5.authenticationService.controllers;

import com.omis5.authenticationService.model.RefreshToken;
import com.omis5.authenticationService.services.RefreshTokenService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@AllArgsConstructor
@RestController
@RequestMapping("/api/token")
public class RefreshTokenController {
    private final RefreshTokenService refreshTokenService;
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class RefreshResponse{
        private String token;
        private Long userId;
        public RefreshResponse(RefreshToken token) {
            this.token = token.getToken();
            this.userId=token.getUser().getId();
        }
    }
    @GetMapping("/refresh")

    public ResponseEntity<RefreshResponse> refreshToken(Long userId) {

        try {
            RefreshToken token = refreshTokenService.generateRefreshTokenById(userId);
            return ResponseEntity.ok(new RefreshResponse(token));

        }catch(Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenService.deleteToken(token);
            throw new RuntimeException("Refresh token expired");
        }
        return token;
    }
}
