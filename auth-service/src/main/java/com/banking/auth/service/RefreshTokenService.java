package com.banking.auth.service;

import com.banking.auth.entity.RefreshToken;
import com.banking.auth.entity.User;
import com.banking.auth.repository.RefreshTokenRepository;
import com.banking.auth.security.JwtTokenProvider;
import com.banking.common.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtTokenProvider jwtTokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // revoke existing refresh tokens for the user to enforce single-session rotation or clean slate
        refreshTokenRepository.revokeAllUserTokens(user);

        String token = jwtTokenProvider.generateRefreshToken();
        Instant expiryDate = Instant.now().plusMillis(jwtTokenProvider.getRefreshExpirationMs());

        RefreshToken refreshToken = new RefreshToken(user, token, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyExpiration(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired. Please login again.");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
