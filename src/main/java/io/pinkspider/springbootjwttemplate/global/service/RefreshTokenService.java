package io.pinkspider.springbootjwttemplate.global.service;

import io.pinkspider.springbootjwttemplate.global.domain.RefreshToken;
import io.pinkspider.springbootjwttemplate.global.repository.RefreshTokenRepository;
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

    @Transactional
    public void delete(String refreshToken) {
        refreshTokenRepository.deleteByRefreshToken(refreshToken);
    }
}
