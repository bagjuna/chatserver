package com.example.chatserver.domain.member.controller;

import com.example.chatserver.domain.member.dto.response.MemberListResponse;
import com.example.chatserver.domain.member.dto.request.LoginRequest;
import com.example.chatserver.domain.member.dto.request.SignupRequest;
import com.example.chatserver.domain.member.service.MemberService;
import com.example.chatserver.global.security.jwt.JwtUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody LoginRequest loginRequest) {
        // email과 password 검증
        log.info("email : {}, password : {}", loginRequest.getEmail(), loginRequest.getPassword());
        memberService.login(loginRequest);

        // 일치할 경우 accessToken 발급
        if(memberService.login(loginRequest) == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "로그인 실패: 이메일 또는 비밀번호가 올바르지 않습니다.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(memberService.login(loginRequest), HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<?> memberList() {
        List<MemberListResponse> dtos = memberService.findAll();
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }



}
