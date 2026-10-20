package io.pinkspider.springboot3jwttemplate.global.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.pinkspider.springboot3jwttemplate.global.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class TokenProvider {

    private final JwtProperties jwtProperties;

    public String generateToken(User user, Duration expiredAt, TokenType tokenType) {
        Date now = new Date();
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user, tokenType);
    }

    private String makeToken(Date expiry, User user, TokenType tokenType) {
        Date now = new Date();

        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .subject(user.getEmail())
                .claim("id", user.getId())
                .claim(TokenType.CLAIM_NAME, tokenType.getValue())
                .signWith(jwtProperties.signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    // 서명·만료뿐 아니라 토큰 용도(token_type)까지 일치해야 유효하다.
    public boolean validToken(String token, TokenType tokenType) {
        try {
            Claims claims = getClaims(token);

            return tokenType.getValue().equals(claims.get(TokenType.CLAIM_NAME, String.class));
        } catch (Exception e) {
            return false;
        }
    }


    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new UsernamePasswordAuthenticationToken(new org.springframework.security.core.userdetails.User(claims.getSubject
                (), "", authorities), token, authorities);
    }

    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims.get("id", Long.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtProperties.signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
