package io.pinkspider.springboot3jwttemplate.global.config.jwt;

import io.jsonwebtoken.Jwts;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

import static java.util.Collections.emptyMap;

@Getter
public class JwtFactory {

    private String subject = "test@email.com";

    private Date issuedAt = new Date();

    private Date expiration = new Date(new Date().getTime() + Duration.ofDays(14).toMillis());

    private Map<String, Object> claims = emptyMap();

    private TokenType tokenType = TokenType.ACCESS;

    @Builder
    public JwtFactory(String subject, Date issuedAt, Date expiration,
                      Map<String, Object> claims, TokenType tokenType) {
        this.subject = subject != null ? subject : this.subject;
        this.issuedAt = issuedAt != null ? issuedAt : this.issuedAt;
        this.expiration = expiration != null ? expiration : this.expiration;
        this.claims = claims != null ? claims : this.claims;
        this.tokenType = tokenType != null ? tokenType : this.tokenType;
    }

    public static JwtFactory withDefaultValues() {
        return JwtFactory.builder().build();
    }

    public String createToken(JwtProperties jwtProperties) {
        return Jwts.builder()
                .subject(subject)
                .header().type("JWT").and()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .claims(claims)
                .claim(TokenType.CLAIM_NAME, tokenType.getValue())
                .signWith(jwtProperties.signingKey(), Jwts.SIG.HS256)
                .compact();
    }
}
