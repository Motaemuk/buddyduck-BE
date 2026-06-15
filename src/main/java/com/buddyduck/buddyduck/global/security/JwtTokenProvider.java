package com.buddyduck.buddyduck.global.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;
	private final Duration accessExpiration;

	public JwtTokenProvider(
		@Value("${jwt.token.secret-key}") String secretKey,
		@Value("${jwt.token.expiration.access}") Long accessExpiration
	) {
		this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
		this.accessExpiration = Duration.ofMillis(accessExpiration);
	}

	public String createAccessToken(AuthUser authUser) {
		Instant now = Instant.now();

		return Jwts.builder()
			.subject(String.valueOf(authUser.userId()))
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(accessExpiration)))
			.signWith(secretKey)
			.compact();
	}

	public Optional<Long> getUserId(String token) {
		try {
			String subject = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();

			return Optional.of(Long.valueOf(subject));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
