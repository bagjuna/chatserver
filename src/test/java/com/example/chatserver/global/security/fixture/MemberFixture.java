package com.example.chatserver.global.security.fixture;

import java.util.UUID;

import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.entity.Role;

public class MemberFixture {

	// 테스트에서 자주 쓰는 기본값으로 생성
	public static Member createDefault() {
		return create("testUser", "test@email.com", "TEST");
	}

	// 커스텀 생성
	public static Member create(String name, String email, String password) {
		// 엔티티의 정적 팩토리 메서드나 빌더를 사용
		// 만약 엔티티가 Protected 생성자만 열어뒀다면, Reflection을 쓰거나
		// 엔티티에 '패키지 프라이빗' 생성자를 열어두고 사용함.
		return Member.builder()
			.email(email)
			.name(name)
			.password(password)
			.publicId(UUID.randomUUID().toString())
			.build();
	}
}
