package com.synex.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtTokenProvider tokenProvider;
  private final JwtAuthenticationFilter jwtAuthFilter;

  public SecurityConfig(JwtTokenProvider tokenProvider,
                        JwtAuthenticationFilter jwtAuthFilter) {
    this.tokenProvider = tokenProvider;
    this.jwtAuthFilter = jwtAuthFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      // disable CSRF if you’re only doing a stateless API
      .csrf(csrf -> csrf.disable())

      // 1) our token endpoint must be open
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.GET, "/api/chat/token").permitAll()
        // if you also want to allow swagger, favicon, etc. you can add more here
        .anyRequest().authenticated()
      )

      // 2) insert our JWT filter so it runs _before_ Spring’s UsernamePasswordAuthFilter
      .addFilterBefore(jwtAuthFilter, 
                       UsernamePasswordAuthenticationFilter.class)

      // 3) when someone isn’t authorized, render our little HTML snippet
      .exceptionHandling(ex -> ex
        .authenticationEntryPoint(html401EntryPoint())
      );

    return http.build();
  }

  // your “show me that pretty red HTML on 401” entry point:
  @Bean
  public AuthenticationEntryPoint html401EntryPoint() {
    return (req, resp, ex) -> {
      resp.setContentType("text/html;charset=UTF-8");
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      resp.getWriter().write("""
        <div style="margin:2em;font-family:sans-serif;color:#a00">
          <h1>Access Denied</h1>
          <p>You must supply a valid JWT to view this application.</p>
          <p>Run in terminal:</p>
          <pre>curl http://localhost:8080/api/chat/token</pre>
          <p>and then open this URL in your browser:</p>
          <pre>http://localhost:8080/Home.html?token=<em>PASTE_YOUR_TOKEN</em></pre>
        </div>
      """);
    };
  }
}
