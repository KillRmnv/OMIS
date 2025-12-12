package com.omis5.authenticationService.services;

import com.omis5.authenticationService.config.JwtTokenGenerator;
import com.omis5.authenticationService.model.AuthenticationUser;
import com.omis5.authenticationService.model.RefreshToken;
import com.omis5.authenticationService.repositories.AuthenticationUserRepository;
import com.omis5.authenticationService.repositories.RefreshTokenRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@AllArgsConstructor
@Service
public class RefreshTokenService {
    private final AuthenticationUserRepository authenticationUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenGenerator jwtTokenGenerator;
    @Value("${jwt.expiration-ms}")
    private long expirationTime;
    public RefreshToken generateRefreshToken(AuthenticationUser user) {
        RefreshToken refreshToken = new RefreshToken();
        return getRefreshToken(refreshToken, user);
    }
    public RefreshToken generateRefreshTokenById(Long userId) {
        RefreshToken refreshToken = new RefreshToken();
       AuthenticationUser user= authenticationUserRepository.findById(userId).orElseThrow();
        return getRefreshToken(refreshToken, user);
    }

    private RefreshToken getRefreshToken(RefreshToken refreshToken, AuthenticationUser user) {
        refreshToken.setUser(user);
        refreshToken.setToken(jwtTokenGenerator.generateToken(user.getUsername(),user.getPassword(),
                user.getRole().toString()));
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(expirationTime));
        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }
    public void deleteToken(RefreshToken refreshToken) {
        try {
            refreshTokenRepository.delete(refreshToken);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
