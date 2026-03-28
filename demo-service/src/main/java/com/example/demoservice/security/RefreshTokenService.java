package com.example.demoservice.security;

import com.example.democommons.exception.InvalidRefreshTokenException;
import com.example.demorepository.entity.RefreshToken;
import com.example.demorepository.entity.User;
import com.example.demorepository.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke any existing refresh tokens for this user
        refreshTokenRepository.deleteAllByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked. Please log in again.");
        }
        return token;
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }
}
