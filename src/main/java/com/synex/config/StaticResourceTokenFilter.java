package com.synex.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // run before Spring Security
public class StaticResourceTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public StaticResourceTokenFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
        throws ServletException, IOException {

        String uri = req.getRequestURI().toLowerCase();

        // 1) Only bootstrap on requests for your HTML/CSS/JS/images:
        if (isStatic(uri) && extractToken(req) == null) {
            // 2) Generate a guest‐JWT
            String guestId = "guest-" + UUID.randomUUID();
            String jwt = tokenProvider.generateGuestToken(guestId);

            // 3) Send it back as a cookie
            Cookie cookie = new Cookie("token", jwt);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            // cookie.setSecure(true); // if you’re on HTTPS
            res.addCookie(cookie);

            // 4) Immediately redirect to the **same** URI so next request carries the cookie
            res.sendRedirect(req.getRequestURI());
            return;  // do NOT proceed to Spring Security on this first pass
        }

        // 5) On all other requests (or second pass), just continue:
        chain.doFilter(req, res);
    }

    private boolean isStatic(String uri) {
        return uri.equals("/")       // if you map "/" → Home.html
            || uri.endsWith(".html")
            || uri.endsWith(".css")
            || uri.endsWith(".js")
            || uri.matches(".*\\.(png|jpe?g|gif)$");
    }

    private String extractToken(HttpServletRequest req) {
        // look in Authorization header
        String hdr = req.getHeader("Authorization");
        if (hdr != null && hdr.startsWith("Bearer "))
            return hdr.substring(7);

        // look in ?token=…
        if (req.getParameter("token") != null)
            return req.getParameter("token");

        // look in cookie
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("token".equals(c.getName()))
                    return c.getValue();
            }
        }
        return null;
    }
}
