package com.example.chatserver.domain.member.controller;

import com.example.chatserver.domain.member.dto.response.MemberListResponse;
import com.example.chatserver.domain.member.dto.request.LoginRequest;
import com.example.chatserver.domain.member.dto.request.SignupRequest;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.service.MemberService;
import com.example.chatserver.global.security.jwt.JwtUtil;
import com.example.chatserver.global.security.userdetails.CustomUserDetails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class MemberController {
    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    public MemberController(MemberService memberService, JwtUtil jwtUtil) {
        this.memberService = memberService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> memberCreate(@RequestBody SignupRequest signupRequest) {
        return memberService.signup(signupRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<?> doLogin(@RequestBody LoginRequest loginRequest) {
        // email과 password 검증
        log.info("email : {}, password : {}", loginRequest.getEmail(), loginRequest.getPassword());
        return memberService.login(loginRequest);
    }

    @GetMapping("/list")
    // admin인 회원만 접근 가능하도록 validation
    public ResponseEntity<?> memberList(@AuthenticationPrincipal CustomUserDetails member) {
        log.info("logged in member: {}", member.getUsername());
        List<MemberListResponse> dtos = memberService.findAll();
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }



}
