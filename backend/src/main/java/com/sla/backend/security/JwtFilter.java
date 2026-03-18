package com.sla.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    public JwtFilter(JwtUtil jwtUtil){
        this.jwtUtil=jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {

        String header = request.getHeader("Authorization");
        String path = request.getRequestURI();
        System.out.println("[JwtFilter] Request to: " + path + ", Auth header: " + (header != null ? "present" : "missing"));

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            System.out.println("[JwtFilter] Token extracted, validating...");

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);
                System.out.println("[JwtFilter] Token valid for: " + email + ", role: " + role);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                System.out.println("[JwtFilter] Token validation failed");
            }
        } else {
            System.out.println("[JwtFilter] No valid Authorization header");
        }
        filterChain.doFilter(request, response);
    }
}
