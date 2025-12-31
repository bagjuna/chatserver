package com.example.chatserver.domain.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chatserver.domain.chat.dto.ChatMessageDto;
import com.example.chatserver.domain.chat.dto.MessageType;
import com.example.chatserver.domain.chat.entity.ChatMessage;
import com.example.chatserver.domain.chat.entity.ChatParticipant;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.chat.repository.ChatMessageRepository;
import com.example.chatserver.domain.chat.repository.ChatParticipantRepository;
import com.example.chatserver.domain.chat.repository.ChatRoomRepository;
import com.example.chatserver.domain.member.entity.Member;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageService {

	private final ChatMessageRepository chatMessageRepository;
	private final ChatParticipantRepository chatParticipantRepository;

	public ChatMessage sendEnterMessage(ChatRoom chatRoom, Member member) {
		ChatMessage enterMessage = ChatMessage.builder()
			.chatRoom(chatRoom)
			.member(member)
			.content(member.getName() + "님이 입장하셨습니다.") // 입장 메시지 내용
			.messageType(MessageType.ENTER)
			.build();

		ChatMessage savedMessage = chatMessageRepository.save(enterMessage);
		chatRoom.updateLastMessage(savedMessage);

		// 4. [작성자님 요청] 여기서 updateReadStatus를 호출!
		// -> 효과: 보낸 사람(나)의 lastReadMessageId가 방금 보낸 메시지로 업데이트됨.
		// -> chatMessageDto의 messageId 필드도 여기서 채워짐.
		updateReadStatus(chatRoom.getRoomId(), member.getPublicId());
		return savedMessage;
	}

	public void updateReadStatus(String roomId, String publicId) {

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
	}
}
