package com.example.chatserver.domain.member.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

	private String name;
	private String email;

	private String profileImage; // 프사 URL (없으면 null)

	@JsonIgnore
	private String password;
	@JsonIgnore
	private LocalDateTime createdAt;
	@JsonIgnore
	private LocalDateTime updatedAt;

	private String accessToken;
}

