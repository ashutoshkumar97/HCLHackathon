package com.hcl.zbankcard.config;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

    	String authHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        // Extract token if present
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception ignored) {
                // Invalid token — let Spring Security reject if the endpoint needs auth
            }
        }

        // If token is valid, populate the security context
        if (username != null && jwtUtil.validateToken(token, username)) {
        	UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.emptyList()
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // Always proceed — Spring Security handles authorization (permitAll vs authenticated)
        chain.doFilter(request, response);
    }
}