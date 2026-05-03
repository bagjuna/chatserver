package com.example.chatserver.domain.member.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.chatserver.domain.member.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/info")
@RequiredArgsConstructor
@Tag(name = "InfoChange", description = "회원 정보 수정")
public class MemberInfoController {
	private final MemberService memberService; // 정보 수정 비즈니스 로직 담당

	@Operation(summary = "프로필 이미지 조회", description = "로그인한 유저의 프로필 이미지 URL을 조회합니다.")
	@GetMapping("/profile-image")
	public ResponseEntity<String> getProfileImage(
		@AuthenticationPrincipal UserDetails userDetails) {
		String imageUrl = memberService.getProfileImage(userDetails.getUsername());
		return ResponseEntity.ok(imageUrl);
	}

	@Operation(summary = "프로필 이미지 변경", description = "로그인한 유저의 프로필 이미지를 S3에 업로드하고 변경합니다.")
	@PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> updateProfileImage(
		@AuthenticationPrincipal UserDetails userDetails,
		@RequestParam("file") MultipartFile file) {

		// 1. 파일이 비어있는지 기본 체크
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body("파일이 존재하지 않습니다.");
		}

		// 2. userDetails.getUsername() (일반적으로 이메일이나 PK)를 넘겨서 식별
		String newImageUrl = memberService.updateProfileImage(userDetails.getUsername(), file);

		return ResponseEntity.ok(newImageUrl);
	}
}

