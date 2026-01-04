package com.example.chatserver.domain.member.service;

import com.example.chatserver.domain.member.dto.response.LoginResponse;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.dto.response.MemberListResponse;
import com.example.chatserver.domain.member.dto.request.LoginRequest;
import com.example.chatserver.domain.member.dto.request.SignupRequest;
import com.example.chatserver.domain.member.entity.Role;
import com.example.chatserver.domain.member.exception.AlreadyExistEmailException;
import com.example.chatserver.domain.member.exception.PasswordIncorrectException;
import com.example.chatserver.domain.member.repository.MemberRepository;
import com.example.chatserver.global.common.error.BaseException;
import com.example.chatserver.global.common.error.ErrorCode;
import com.example.chatserver.global.security.jwt.JwtUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final JwtUtil jwtUtil;
	private final StringRedisTemplate redisTemplate;
	private final PasswordEncoder passwordEncoder;

	public MemberService(MemberRepository memberRepository, JwtUtil jwtUtil, StringRedisTemplate redisTemplate,
		PasswordEncoder passwordEncoder) {
		this.memberRepository = memberRepository;
		this.jwtUtil = jwtUtil;
		this.redisTemplate = redisTemplate;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public ResponseEntity<LoginResponse> signup(SignupRequest signupRequest) {
		// 이미 가입되어 있는 회원인지 확인
		if (memberRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
			// 중복 이메일 에러 throws
			throw new AlreadyExistEmailException();
		}

		Member newMember = Member.builder()
			.name(signupRequest.getName())
			.email(signupRequest.getEmail())
			.password(passwordEncoder.encode(signupRequest.getPassword()))
			.role(Role.USER)
			.publicId("user_" + java.util.UUID.randomUUID())
			.build();

		memberRepository.save(newMember);

		return getAccessToken(newMember);

	}

	public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
		Member member = memberRepository.findByEmail(loginRequest.getEmail())
			.orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));

		if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
			throw new PasswordIncorrectException();
		}

		return getAccessToken(member);
	}

	public List<MemberListResponse> findAll() {
		List<Member> members = memberRepository.findAll();
		List<MemberListResponse> memberListResponses = members.stream()
			.map(m -> new MemberListResponse(m.getId(), m.getName(), m.getEmail()))
			.toList();
		return memberListResponses;
	}

	private ResponseEntity<LoginResponse> getAccessToken(Member member) {
		String accessToken = jwtUtil.createAccessToken(member.getPublicId());
		String refreshToken = jwtUtil.createRefreshToken(member.getPublicId());
		// Redis에 Refresh Token 저장
		// Key: "RT:1", Value: "eyJ...", Duration: 30일
		redisTemplate.opsForValue().set(
			"RT:" + member.getPublicId(),
			refreshToken,
			jwtUtil.getRefreshTokenMaxAgeInSeconds(), //  2592000초 (30일)
			java.util.concurrent.TimeUnit.SECONDS
		);

		// 2. Refresh Token은 쿠키로!
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
			.path("/")
			.sameSite("None")
			.secure(true) // HTTPS 사용 시
			.httpOnly(true)
			.maxAge(jwtUtil.getRefreshTokenMaxAgeInSeconds())
			.build();

		return ResponseEntity.ok()
			.header("Set-Cookie", refreshCookie.toString())
			.body(LoginResponse.builder() // 로그인 응답 DTO 재사용
				.email(member.getEmail())
				.name(member.getName())
				.accessToken(accessToken)
				.build()
			);

	}

	public ResponseEntity<LoginResponse> reissueToken(String refreshToken) {
		// 1. Refresh Token 검증
		jwtUtil.validateToken(refreshToken);

		// 2. Refresh Token에서 Public ID 추출
		String publicId = jwtUtil.getUserIdFromToken(refreshToken);
		// 3. Public ID로 회원 조회
		Member member = memberRepository.findByPublicId(publicId).orElseThrow(
			() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND)
		);

		// 4. Redis에서 저장된 Refresh Token과 비교
		String storedRefreshToken = redisTemplate.opsForValue().get("RT:" + publicId);
		if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
			throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		// 5. 새로운 Access Token 생성
		String newAccessToken = jwtUtil.createAccessToken(publicId);

		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
			.path("/")
			.sameSite("None")
			.secure(true) // HTTPS 사용 시
			.httpOnly(true)
			.maxAge(jwtUtil.getRefreshTokenMaxAgeInSeconds())
			.build();

		return ResponseEntity.ok()
			.header("Set-Cookie", refreshCookie.toString())
			.body(LoginResponse.builder()
				.email(member.getEmail())
				.name(member.getName())
				.accessToken(newAccessToken)
				.build()
			);
	}

}
