package com.example.chatserver.domain.member.controller;

import com.example.chatserver.domain.member.dto.response.MemberListResponse;
import com.example.chatserver.domain.member.dto.request.LoginRequest;
import com.example.chatserver.domain.member.dto.request.SignupRequest;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.service.MemberService;
import com.example.chatserver.global.security.userdetails.CustomUserDetails;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class MemberController {
	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
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


	// 토큰 재발급
	@PostMapping("/reissue")
	public ResponseEntity<?> reissueToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
		if (refreshToken == null) {
			// 쿠키가 아예 없는 경우 (로그인 안 한 상태)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token is missing");
		}

		// 서비스 로직 호출 및 응답 반환
		return memberService.reissueToken(refreshToken);
	}

	@PreAuthorize("hasRole('ADMIN')") // ADMIN만 실행 가능
	@GetMapping("/list")
	// admin인 회원만 접근 가능하도록 validation
	public ResponseEntity<?> memberList(@AuthenticationPrincipal CustomUserDetails member) {
		log.info("logged in member: {}", member.getUsername());
		List<MemberListResponse> dtos = memberService.findAll();

		return new ResponseEntity<>(dtos, HttpStatus.OK);
	}

}
