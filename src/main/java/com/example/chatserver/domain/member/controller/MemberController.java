package com.example.chatserver.domain.member.controller;

import com.example.chatserver.domain.member.dto.response.LoginResponse;
import com.example.chatserver.domain.member.dto.response.MemberListResponse;
import com.example.chatserver.domain.member.dto.request.LoginRequest;
import com.example.chatserver.domain.member.dto.request.SignupRequest;
import com.example.chatserver.domain.member.service.MemberService;
import com.example.chatserver.global.common.error.BaseException;
import com.example.chatserver.global.common.error.ErrorCode;
import com.example.chatserver.global.security.userdetails.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth", description = "로그인/회원가입/토큰재발급/회원목록")
public class MemberController {
	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@Operation(
		summary = "회원가입 API",
		description = "회원 이메일, 비밀번호, 이름을 받아서 회원을 등록합니다."
	)
	@PostMapping("/signup")
	public ResponseEntity<LoginResponse> memberCreate(@RequestBody SignupRequest signupRequest) {
		return memberService.signup(signupRequest);
	}

	@Operation(
		summary = "로그인 API",
		description = "회원 이메일과 비밀번호를 받아서 로그인 처리합니다."
	)
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> doLogin(@RequestBody LoginRequest loginRequest) {
		log.info("Login attempt for email: {}", loginRequest.getEmail());
		// email과 password 검증
		return memberService.login(loginRequest);
	}

	// 토큰 재발급
	@Operation(
		summary = "토큰 재발급 API",
		description = "쿠키에 저장된 리프레시 토큰을 받아서 액세스 토큰과 리프레시 토큰을 재발급합니다."
	)
	@PostMapping("/reissue")
	public ResponseEntity<LoginResponse> reissueToken(
		@CookieValue(name = "refreshToken", required = false) String refreshToken) {
		log.info("Token reissue attempt with refresh token: {}", refreshToken);
		if (refreshToken == null) {
			// 쿠키가 아예 없는 경우 (로그인 안 한 상태)
			throw new BaseException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
		}

		// 서비스 로직 호출 및 응답 반환
		return memberService.reissueToken(refreshToken);
	}

	@Operation(summary = "회원 목록 조회 (관리자)", description = "관리자 권한을 가진 사용자만 접근 가능합니다.")
	@PreAuthorize("hasRole('ADMIN')") // ADMIN만 실행 가능
	@GetMapping("/list")
	// admin인 회원만 접근 가능하도록 validation
	public ResponseEntity<List<MemberListResponse>> memberList(@AuthenticationPrincipal CustomUserDetails member) {
		List<MemberListResponse> dtos = memberService.findAll();

		return new ResponseEntity<>(dtos, HttpStatus.OK);
	}

}
