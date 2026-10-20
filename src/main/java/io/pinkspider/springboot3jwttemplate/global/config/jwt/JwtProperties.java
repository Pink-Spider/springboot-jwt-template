package io.pinkspider.springboot3jwttemplate.global.config.jwt;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Setter
@Data
@Component
@ConfigurationProperties("jwt")
public class JwtProperties {
    private String issuer;
    private String secretKey;

    // HMAC-SHA 서명 키. jjwt 0.12+ 는 String 키를 받지 않고 256bit 이상의 SecretKey 를 요구한다.
    public SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
