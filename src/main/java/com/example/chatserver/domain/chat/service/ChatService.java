package com.example.chatserver.domain.chat.service;

import com.example.chatserver.domain.chat.dto.ChatParticipantDto;
import com.example.chatserver.domain.chat.dto.request.ChatMessageSearch;
import com.example.chatserver.domain.chat.entity.ChatMessage;
import com.example.chatserver.domain.chat.entity.ChatParticipant;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.chat.dto.ChatMessageDto;
import com.example.chatserver.domain.chat.repository.ChatMessageRepository;
import com.example.chatserver.domain.chat.repository.ChatParticipantRepository;
import com.example.chatserver.domain.chat.repository.ChatRoomRepository;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.repository.MemberRepository;
import com.example.chatserver.global.common.error.BaseException;
import com.example.chatserver.global.common.error.ErrorCode;
import com.example.chatserver.global.common.paging.PageRequestDTO;
import com.example.chatserver.global.common.paging.PageResponseDTO;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatParticipantRepository chatParticipantRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final MemberRepository memberRepository;
	private final ChatParticipantService chatParticipantService;
	private final ChatMessageService chatMessageService;

	public ChatMessageDto saveMessage(String roomId, ChatMessageDto chatMessageDto) {
		// 1. 채팅방 및 보낸 사람 조회
		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
			() -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
		);
		Member sender = memberRepository.findByEmail(chatMessageDto.getSenderEmail()).orElseThrow(
			() -> new EntityNotFoundException("보낸 사람을 찾을 수 없습니다.")
		);
		// 2. 메시지 저장
		ChatMessage chatMessage = ChatMessage.builder()
			.chatRoom(chatRoom)
			.member(sender)
			.content(chatMessageDto.getMessage())
			.messageType(chatMessageDto.getMessageType())
			.build();
		ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

		// 3. 방의 마지막 메시지 정보 갱신 (반정규화)
		chatRoom.updateLastMessage(savedMessage);

		// 4. [작성자님 요청] 여기서 updateReadStatus를 호출!
		// -> 효과: 보낸 사람(나)의 lastReadMessageId가 방금 보낸 메시지로 업데이트됨.
		// -> chatMessageDto의 messageId 필드도 여기서 채워짐.
		updateReadStatus(roomId, sender.getPublicId(), chatMessageDto);


		return chatMessageDto;
	}

	public void addParticipantToGroupChat(String roomId) {
		// 채팅방 조회
		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
			() -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
		);
		// member 조회
		Member member = memberRepository.findByPublicId(SecurityContextHolder.getContext()
			.getAuthentication().getName()).orElseThrow(
			() -> new EntityNotFoundException("사용자를 찾을 수 없습니다.")
		);

		if (!chatRoom.isGroupChat()) {
			throw new IllegalArgumentException("그룹채팅이 아닙니다.");
		}

		// 이미 참여자인지 검증
		Optional<ChatParticipant> participant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member);
		if (participant.isEmpty()) {
			chatParticipantService.addParticipantMember(chatRoom, member);
		}
		chatMessageService.sendEnterMessage(chatRoom, member);

	}

	public void addParticipantToPrivateChat(String roomId, String password, String publicId) {
		// 채팅방 조회
		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
			() -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
		);
		// member 조회
		Member member = memberRepository.findByPublicId(publicId).orElseThrow(
			() -> new EntityNotFoundException("사용자를 찾을 수 없습니다.")
		);
		if (chatRoom.isSecret()) {
			if (!chatRoom.getPassword().equals(password)) {
				throw new BaseException(ErrorCode.CHAT_ROOM_PASSWORD_INCORRECT);
			}

		}

		chatParticipantService.addParticipantMember(chatRoom, member);

	}

	public List<ChatMessageDto> getChatHistory(String roomId) {
		// 내가 해당 채팅방의 참여자가 아닐 경우 에러
		// 채팅방 조회
		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
			() -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
		);
		// 참여자 조회
		Member member = memberRepository.findByPublicId(SecurityContextHolder.getContext()
			.getAuthentication().getName()).orElseThrow(
			() -> new EntityNotFoundException("사용자를 찾을 수 없습니다.")
		);

		chatParticipantService.isParticipantMember(chatRoom.getId(), member.getId());

		// 특정 room에 대한 message 조회
		List<ChatMessage> messages = chatMessageRepository.findAllByRoomIdWithSender(roomId);
		List<Long> readCursors = chatParticipantRepository.findReadCursorsByRoomId(roomId);

		// 3. 각 메시지별로 안 읽은 사람 수 계산
		return messages.stream().map(msg -> {
			// 내 메시지 ID보다 '작은' 커서를 가진 사람 수 = 아직 이 메시지를 못 본 사람
			long unreadCount = readCursors.stream()
				.filter(cursor -> cursor < msg.getId())
				.count();

			return ChatMessageDto.builder()
				.roomId(roomId)
				.senderEmail(msg.getMember().getEmail())
				.senderName(msg.getMember().getName())
				.message(msg.getContent())
				.createdAt(msg.getCreatedTime())
				.messageId(msg.getId())
				.unreadCount((int)unreadCount) // 계산된 수치 주입
				.build();
		}).collect(Collectors.toList());

	}

	public PageResponseDTO<ChatMessageDto> getChatHistoryPaged(
		ChatMessageSearch chatMessageSearch,
		PageRequestDTO pageRequestDTO,
		Member member) {
		// // 내가 해당 채팅방의 참여자가 아닐 경우 에러
		// // 채팅방 조회
		// ChatRoom chatRoom = chatRoomRepository.findByRoomId(chatMessageSearch.getRoomId()).orElseThrow(
		// 	() -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
		// );
		//
		// chatParticipantService.isParticipantMember(chatRoom.getId(), member.getId());
		//
		// // 페이징된 메시지 조회
		// PageResponseDTO<ChatMessageDto> pagedMessages = chatMessageRepository.findPagedMessagesByRoomId(
		// 	chatMessageSearch, pageRequestDTO);
		//
		// // 3. 각 메시지별로 안 읽은 사람 수 계산
		// List<Long> readCursors = chatParticipantRepository.findReadCursorsByRoomId(chatMessageSearch.getRoomId());
		//
		// pagedMessages.getDtoList().forEach(msgDto -> {
		// 	// 내 메시지 ID보다 '작은' 커서를 가진 사람 수 = 아직 이 메시지를 못 본 사람
		// 	long unreadCount = readCursors.stream()
		// 		.filter(cursor -> cursor < msgDto.getMessageId())
		// 		.count();
		// 	msgDto.setUnreadCount((int)unreadCount); // 계산된 수치 주입
		// });
		//
		// return pagedMessages;
		return new PageResponseDTO<>();
	}

	public boolean isRoomParticipant(String publicId, String roomId) {
		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
			.orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));
		Member member = memberRepository.findByPublicId(publicId)
			.orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

		return chatParticipantService.isParticipantMember(chatRoom.getId(), member.getId());
	}

	public void messageRead(String roomId, ChatMessageDto chatMessageDto) {
		Member sender = memberRepository.findByEmail(chatMessageDto.getSenderEmail()).orElseThrow(
			() -> new EntityNotFoundException("보낸 사람을 찾을 수 없습니다.")
		);
		updateReadStatus(roomId, sender.getPublicId(), chatMessageDto);


	}



	public void updateReadStatus(String roomId, String publicId, ChatMessageDto chatMessageDto) {

		// Fetch Join으로 방 정보까지 한 번에 가져옴
		ChatParticipant participant = chatParticipantRepository.findByRoomIdAndMemberPublicId
				(roomId, publicId)
			.orElseThrow(
				() -> new EntityNotFoundException("참여방을 찾을 수 없습니다.")
			);

		// 2. 방의 최신 메시지 ID 가져오기 (캐싱된 데이터 활용 - 성능 최적화)
		Long currentLastMessageId = participant.getChatRoom().getLastMessageId();

		// 3. 커서 업데이트 (내 커서가 최신보다 뒤처져 있을 때만)
		// 주의: currentLastMessageId가 null이면(메시지가 없는 방) 0L 반환
		if (currentLastMessageId == null) {
			return ;
		}

		if (participant.getLastReadMessageId() == null || participant.getLastReadMessageId() < currentLastMessageId) {
			participant.updateLastReadMessage(currentLastMessageId);
			// Dirty Checking으로 자동 저장됨
		}

		// 4. [중요] 업데이트된 ID 반환 (Controller에서 브로드캐스팅용으로 사용)

		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
			() -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
		);
		int totalParticipants = chatParticipantRepository.countByChatRoom(chatRoom);
		int unreadCount = (totalParticipants > 0) ? totalParticipants - 1 : 0;

		chatMessageDto.updateMessageId(currentLastMessageId, unreadCount);
	}

	public List<ChatParticipantDto> getChatParticipants(String roomId) {
		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
			() -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
		);

		List<ChatParticipant> participants = chatParticipantRepository.findByChatRoom(chatRoom);

		return participants.stream().map(participant -> ChatParticipantDto.builder()
			.email(participant.getMember().getEmail())
			.name(participant.getMember().getName())
			.lastReadMessageId(participant.getLastReadMessageId())
			.build()
		).collect(Collectors.toList());
	}
}
