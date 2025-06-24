package com.synex.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider tokenProvider;

	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {

		String jwt = null;

		// 1) Try the Authorization header
		String bearer = req.getHeader("Authorization");
		if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
			jwt = bearer.substring(7);
		}

		// 2) Otherwise try the accessToken cookie
		if (jwt == null && req.getCookies() != null) {
			for (Cookie c : req.getCookies()) {
				if ("accessToken".equals(c.getName())) {
					jwt = c.getValue();
					break;
				}
			}
		}

		// 3) If we got a token and it checks out, set authentication
		if (jwt != null && tokenProvider.validateToken(jwt)) {
			String user = tokenProvider.getSubjectFromToken(jwt);
			Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
			SecurityContextHolder.getContext().setAuthentication(auth);
		}

		chain.doFilter(req, res);
	}
}
