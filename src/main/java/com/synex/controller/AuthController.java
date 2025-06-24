package com.synex.controller;

import com.synex.config.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticationManager authManager;
	private final JwtTokenProvider tokenProvider;

	@Value("${app.jwtAccessExpirationMs}")
	private long accessExpireMs;

	@Value("${app.jwtRefreshExpirationMs}")
	private long refreshExpireMs;

	public AuthController(AuthenticationManager authManager, JwtTokenProvider tokenProvider) {
		this.authManager = authManager;
		this.tokenProvider = tokenProvider;
	}

	/**
	 * Login endpoint: - authenticates the one in-memory user - issues access &
	 * refresh tokens as cookies
	 */
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, String> creds, HttpServletResponse response) {
		// 1) Authenticate credentials
		Authentication auth = authManager
				.authenticate(new UsernamePasswordAuthenticationToken(creds.get("username"), creds.get("password")));
		SecurityContextHolder.getContext().setAuthentication(auth);

		String subject = auth.getName();
		String accessToken = tokenProvider.generateAccessToken(subject);
		String refreshToken = tokenProvider.generateRefreshToken(subject);

		// 2) Set refreshToken cookie (HttpOnly, path=/api/auth/refresh)
		ResponseCookie rtCookie = ResponseCookie.from("refreshToken", refreshToken).httpOnly(true).secure(true)
				.path("/api/auth/refresh").maxAge(refreshExpireMs / 1000).sameSite("Strict").build();
		response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

		// 3) Set accessToken cookie (readable by JS, path=/)
		ResponseCookie atCookie = ResponseCookie.from("accessToken", accessToken).httpOnly(false).secure(true).path("/")
				.maxAge(accessExpireMs / 1000).sameSite("Strict").build();
		response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());

		// 4) Return success payload
		return ResponseEntity.ok(Map.of("message", "Login successful"));
	}

	/**
	 * Refresh endpoint: - reads HttpOnly refreshToken cookie - if valid, issues new
	 * access & refresh tokens
	 */
	@PostMapping("/refresh")
	public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken,
			HttpServletResponse response) {
		if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
		}

		String subject = tokenProvider.getSubjectFromToken(refreshToken);
		String newRefresh = tokenProvider.generateRefreshToken(subject);

		// rotate cookies
		ResponseCookie rtCookie = ResponseCookie.from("refreshToken", newRefresh).httpOnly(true).secure(false)
				.path("/api/auth/refresh").maxAge(refreshExpireMs / 1000).sameSite("Strict").build();
		response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());

		return ResponseEntity.ok(Map.of("message", "Token refreshed"));
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpServletResponse response) {
		// 1) Clear the accessToken cookie (JS‐readable)
		ResponseCookie clearAt = ResponseCookie.from("accessToken", "").httpOnly(false).secure(false) // match your dev
																										// secure flag
				.path("/").maxAge(0).sameSite("Strict").build();
		response.addHeader(HttpHeaders.SET_COOKIE, clearAt.toString());

		// 2) Clear the refreshToken cookie (HttpOnly)
		ResponseCookie clearRt = ResponseCookie.from("refreshToken", "").httpOnly(true).secure(false)
				.path("/api/auth/refresh").maxAge(0).sameSite("Strict").build();
		response.addHeader(HttpHeaders.SET_COOKIE, clearRt.toString());

		// 3) (Optional) Invalidate SecurityContext
		SecurityContextHolder.clearContext();

		return ResponseEntity.ok(Map.of("message", "Logged out"));
	}

}
