package com.example.chatserver.domain.chat.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.example.chatserver.AbstractIntegrationTest;
import com.example.chatserver.domain.chat.dto.request.ChatRoomCreate;
import com.example.chatserver.domain.chat.repository.ChatParticipantRepository;
import com.example.chatserver.domain.chat.repository.ChatRoomRepository;
import com.example.chatserver.domain.chat.repository.ReadStatusRepository;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.repository.MemberRepository;
import com.example.chatserver.global.security.fixture.MemberFixture;
import com.example.chatserver.global.security.jwt.JwtUtil;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class ChatRoomServiceTest extends AbstractIntegrationTest {

	@Autowired
	private ChatRoomService chatRoomService;

	@Autowired
	private MockMvcTester mockMvc;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatParticipantRepository chatParticipantRepository;

	@Autowired
	private ReadStatusRepository readStatusRepository;

	private Member testUser;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
		chatRoomRepository.deleteAll();
		chatParticipantRepository.deleteAll();
		readStatusRepository.deleteAll();
		testUser = MemberFixture.create("testUser", "test@email.com", "TEST");
		memberRepository.save(testUser);
	}

	@Test
	@DisplayName("채팅방 생성 테스트")
	void createChatRoomTest() {
		ChatRoomCreate chatRoomCreate = ChatRoomCreate.builder()
			.roomName("Test Room")
			.isSecretChat(false)
			.build();
		String groupRoomId = chatRoomService.createGroupRoom(chatRoomCreate, testUser);
		assertEquals(1, chatRoomRepository.count());
		assertEquals(1, chatParticipantRepository.count());

	}




}
