package com.synex.controller;

import com.synex.config.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class TokenController {

	private final JwtTokenProvider tokenProvider;

	public TokenController(JwtTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	/**
	 * GET /api/chat/token Generates a short‐lived JWT (using generateAccessToken)
	 * with a random guest ID as subject.
	 */
	@GetMapping("/token")
	public Map<String, String> token() {
		// use a random UUID so each visitor gets a unique “subject”
		String guestId = UUID.randomUUID().toString();

		// issue an access‐token (e.g. 15m expiry)
		String jwt = tokenProvider.generateAccessToken(guestId);

		return Collections.singletonMap("token", jwt);
	}
}
