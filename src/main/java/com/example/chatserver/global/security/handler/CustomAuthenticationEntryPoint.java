package com.example.chatserver.global.security.handler;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.example.chatserver.global.common.error.ErrorCode;
import com.example.chatserver.global.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws
        IOException, ServletException {
        ErrorResponse<Void> errorResponse;

        String token = request.getHeader("Authorization"); // Bearer 토큰 여부 확인
        if (token == null || token.isEmpty()) {
            // 인증 자체 없음
            errorResponse = ErrorResponse.of(ErrorCode.AUTHENTICATION_REQUIRED, request.getRequestURI(), null);
        } else {
            // 인증 시도는 있었으나 실패
            errorResponse = ErrorResponse.of(ErrorCode.JWT_AUTHENTICATION_FAIL, request.getRequestURI(), null);
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
