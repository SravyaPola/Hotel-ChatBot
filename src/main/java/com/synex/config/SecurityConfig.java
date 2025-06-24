package com.synex.config;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
		this.jwtAuthFilter = jwtAuthFilter;
	}

	// <-- Add this bean so Spring can inject AuthenticationManager
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public UserDetailsService userDetailsService(@Value("${app.auth.username}") String username,
			@Value("${app.auth.password}") String password) {
		// Because we’re registering a NoOpPasswordEncoder below,
		// we can store the raw password directly.
		UserDetails user = User.withUsername(username).password(password).roles("USER").build();
		return new InMemoryUserDetailsManager(user);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)

				.authorizeHttpRequests(auth -> auth
						// public endpoints
						.requestMatchers("/login.html", "/js/**", "/css/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/chat/token").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh").permitAll()
						// everything else requires JWT
						.anyRequest().authenticated())

				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(ex -> ex.authenticationEntryPoint(this::commence));

		return http.build();
	}

	private void commence(HttpServletRequest req, HttpServletResponse res,
			org.springframework.security.core.AuthenticationException ex) throws IOException {
		String uri = req.getRequestURI();
		if (uri.startsWith("/api/")) {
			res.setContentType("application/json");
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			res.getWriter().write("{\"error\":\"Unauthorized\"}");
		} else {
			res.sendRedirect("/login.html");
		}
	}

	@Bean
	@SuppressWarnings("deprecation")
	public PasswordEncoder passwordEncoder() {
		return NoOpPasswordEncoder.getInstance();
	}
}
