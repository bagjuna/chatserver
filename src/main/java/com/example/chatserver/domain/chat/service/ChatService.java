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
	private final ChatParticipantService chatParticipantService;
	private final ChatMessageService chatMessageService;
	private final MemberRepository memberRepository;

	public ChatMessageDto saveMessage(String roomId, ChatMessageDto chatMessageDto) {
		// 1. 채팅방 및 보낸 사람 조회
		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
			() -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
		);
		Member sender = memberRepository.findByEmail(chatMessageDto.getSenderEmail()).orElseThrow(
			() -> new EntityNotFoundException("보낸 사람을 찾을 수 없습니다.")
		);
		// 2. 메시지 저장
		// ChatMessage chatMessage = ChatMessage.builder()
		// 	.chatRoom(chatRoom)
		// 	.member(sender)
		// 	.content(chatMessageDto.getMessage())
		// 	.messageType(chatMessageDto.getMessageType())
		// 	.build();
		chatMessageService.saveMessage(chatRoom, sender, chatMessageDto);
		chatMessageService.updateReadStatus(roomId, sender.getPublicId(), chatMessageDto);


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

	public List<ChatMessageDto> getChatHistory(String roomId,String publicId) {
		// 내가 해당 채팅방의 참여자가 아닐 경우 에러
		// 채팅방 조회
		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
			() -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
		);
		// 참여자 조회
		Member member = memberRepository.findByPublicId(publicId).orElseThrow(
			() -> new EntityNotFoundException("사용자를 찾을 수 없습니다.")
		);

		chatParticipantService.isParticipantMember(chatRoom.getId(), member.getId());

		// 특정 room에 대한 message 조회
		List<ChatMessage> messages = chatMessageService.getMessagesByRoomId(roomId);
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
		chatMessageService.updateReadStatus(roomId, sender.getPublicId(), chatMessageDto);


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
