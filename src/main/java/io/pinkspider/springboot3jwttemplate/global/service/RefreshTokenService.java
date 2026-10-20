package io.pinkspider.springboot3jwttemplate.global.service;

import io.pinkspider.springboot3jwttemplate.global.domain.RefreshToken;
import io.pinkspider.springboot3jwttemplate.global.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken findByRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Unexpected token"));
    }

    @Transactional
    public RefreshToken saveOrUpdate(Long userId, String refreshToken) {
        RefreshToken entity = refreshTokenRepository.findByUserId(userId)
                .map(existing -> existing.update(refreshToken))
                .orElseGet(() -> new RefreshToken(userId, refreshToken));

        return refreshTokenRepository.save(entity);
    }
}
