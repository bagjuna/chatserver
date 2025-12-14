package com.example.chatserver.global.security.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.example.chatserver.AbstractIntegrationTest;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.repository.MemberRepository;
import com.example.chatserver.global.security.fixture.MemberFixture;
import com.example.chatserver.global.security.jwt.JwtUtil;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class SpringSecurityControllerTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private MemberRepository memberRepository;

	private Member testUser;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
		testUser = MemberFixture.create("testUser", "test@email.com", "TEST");
		memberRepository.save(testUser);
	}

	@Test
	@DisplayName("익명 사용자는 /test/anonymous에 접근 가능")
	@WithAnonymousUser
	void anonymousUserCanAccessAnonymousEndpoint() {
		assertThat(mockMvc.get().uri("/test/anonymous"))
			.hasStatusOk();
	}

	@Test
	@WithMockUser
	@DisplayName("인증된 사용자는 /test/anonymous에 접근 가능")
	void authenticatedMemberCanAccessAnonymousEndpoint() {
		assertThat(mockMvc.get().uri("/test/anonymous"))
			.hasStatusOk();
	}

	@Test
	@DisplayName("익명 사용자는 /test/anonymous에 접근 불가, 401 반환")
	@WithAnonymousUser
	void anonymousUserCannotAccessAuthenticatedEndpoint() {
		assertThat(mockMvc.get().uri("/test/authenticated"))
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@WithMockUser
	@DisplayName("인증된 사용자는 /test/authenticated에 접근 가능")
	void authenticatedMemberCanAccessAuthenticatedEndpoint() {
		assertThat(mockMvc.get().uri("/test/authenticated"))
			.hasStatusOk();
	}

	@Test
	@DisplayName("유효한 JWT 엑세스 토큰으로 /test/authenticated에 접근 가능")
	void jwtCanBeValidated() {
		String accessToken = jwtUtil.createAccessToken(testUser.getPublicId());
		assertThat(mockMvc.get()
			.header("Authorization", "Bearer " + accessToken)
			.uri("/test/authenticated"))
			.hasStatusOk();
	}

	@Test
	@DisplayName("유효하지 않은 JWT 엑세스 토큰인 경우 /test/authenticated에 접근 불가")
	void withoutJwtHeader() {
		assertThat(mockMvc.get()
			.header("Authorization", "Bearer " + "Hello World.")
			.uri("/test/authenticated"))
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
