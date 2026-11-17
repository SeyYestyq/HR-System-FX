package ru.mirea.hrsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import ru.mirea.hrsystem.config.AppProperties;
import ru.mirea.hrsystem.domain.UserAccount;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final AppProperties properties;
    private final SecretKey key;

    public JwtService(AppProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.jwt().secretBase64()));
    }

    public String createAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.jwt().accessMinutes() * 60);
        return Jwts.builder()
                .subject(user.getUuid().toString())
                .issuer(properties.jwt().issuer())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .claim("mustChangePassword", user.isMustChangePassword())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.jwt().issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long accessTtlSeconds() {
        return properties.jwt().accessMinutes() * 60;
    }
}
