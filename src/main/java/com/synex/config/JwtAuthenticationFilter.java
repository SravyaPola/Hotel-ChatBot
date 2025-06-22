package com.synex.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
// … your imports …

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
        throws ServletException, IOException {

      String jwt = null;
      String hdr = req.getHeader("Authorization");
      if (StringUtils.hasText(hdr) && hdr.startsWith("Bearer ")) {
        jwt = hdr.substring(7);
      }
      else if (req.getParameter("token") != null) {
        jwt = req.getParameter("token");
      }
      else if (req.getCookies() != null) {
        for (Cookie c : req.getCookies()) {
          if ("token".equals(c.getName())) {
            jwt = c.getValue();
            break;
          }
        }
      }

      // ONLY if valid do we set an Authentication
      if (jwt != null && tokenProvider.validateToken(jwt)) {
        String subject = tokenProvider.getSubjectFromToken(jwt);
        Authentication auth = new UsernamePasswordAuthenticationToken(subject,
                                                                      null,
                                                                      List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
      }

      chain.doFilter(req, res);
    }
}
