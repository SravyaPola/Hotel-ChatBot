package com.synex.controller;

import com.synex.config.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
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

	@GetMapping("/token")
	public Map<String, String> token() {
		String guestId = UUID.randomUUID().toString();
		String jwt = tokenProvider.generateGuestToken(guestId);
		return Collections.singletonMap("token", jwt);
	}
}