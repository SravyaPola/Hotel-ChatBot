package com.synex.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

	@Value("${app.jwtSecret}")
	private String jwtSecret;

	@Value("${app.jwtAccessExpirationMs}")
	private long jwtAccessExpirationMs;

	@Value("${app.jwtRefreshExpirationMs}")
	private long jwtRefreshExpirationMs;

	private Key key;

	@PostConstruct
	public void init() {
		// Build an HMAC-SHA256 key from your base64-encoded secret
		this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
	}

	/** Issue a short-lived access token (e.g. 15m) */
	public String generateAccessToken(String subject) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtAccessExpirationMs);
		return Jwts.builder().setSubject(subject).setIssuedAt(now).setExpiration(expiry)
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	/** Issue a long-lived refresh token (e.g. 7d) */
	public String generateRefreshToken(String subject) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtRefreshExpirationMs);
		return Jwts.builder().setSubject(subject).setIssuedAt(now).setExpiration(expiry)
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	/** Extract the “sub” (your username) from a valid token */
	public String getSubjectFromToken(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
	}

	/** Validate signature + expiry */
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}
}
