package com.coworkinghub.coworkinghub.security;

import com.coworkinghub.coworkinghub.model.Compte;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Issues and verifies HS256 JWTs whose subject is the account id. */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration:PT24H}") Duration expiration) {
        this.expiration = expiration;
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET is not set: using a random key. Tokens will stop working when the server restarts.");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long");
            }
            this.key = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String createToken(Compte compte) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(compte.getId()))
                .claim("type", compte.getTypeCompte() == null ? null : compte.getTypeCompte().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * @return the verified claims
     * @throws JwtException if the token is malformed, tampered with or expired
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
