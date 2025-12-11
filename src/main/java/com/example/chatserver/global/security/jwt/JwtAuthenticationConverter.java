package com.example.chatserver.global.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter implements AuthenticationConverter {

    @Override
    public JwtAuthenticationToken convert(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring(7);
        if (token.isEmpty()) {
            return null;
        }

        return JwtAuthenticationToken.unauthenticated(token);
    }
}
