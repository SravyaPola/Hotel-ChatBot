package com.synex.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

	@Value("${app.jwtSecret}")
	private String jwtSecret;

	@Value("${app.jwtExpirationMs}")
	private long jwtExpirationMs;

	private Key key;

	@PostConstruct
	public void init() {
		key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
	}

	/** Generate a guest token (subject = guestId) */
	public String generateGuestToken(String guestId) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtExpirationMs);
		return Jwts.builder().setSubject(guestId).setIssuedAt(now).setExpiration(expiry)
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	/** Parse subject (guestId) */
	public String getSubjectFromToken(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
	}

	/** Validate signature & expiration */
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}
}
