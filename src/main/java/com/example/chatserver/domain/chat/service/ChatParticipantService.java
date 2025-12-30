package com.example.chatserver.domain.chat.service;


import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chatserver.domain.chat.dto.ChatMessageDto;
import com.example.chatserver.domain.chat.entity.ChatParticipant;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.chat.entity.RoomRole;
import com.example.chatserver.domain.chat.repository.ChatParticipantRepository;
import com.example.chatserver.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class ChatParticipantService {

	private final ChatParticipantRepository chatParticipantRepository;

	@Transactional(readOnly = true)
	public List<ChatParticipant> getParticipantsByMember(Member member) {
		return chatParticipantRepository.findAllByMemberWithRoom(member);
	}

	public void addParticipantMember(ChatRoom chatRoom, Member member) {

		if(isParticipantMember(chatRoom.getId(), member.getId())) {
			return;
		}
		if (chatRoom.canJoin()) {
			chatRoom.increaseParticipantCount();
		}

		ChatParticipant chatParticipant = ChatParticipant.builder()
			.chatRoom(chatRoom)
			.member(member)
			.roomRole(RoomRole.NORMAL)
			.build();
		chatParticipantRepository.save(chatParticipant);
	}

	public void addParticipantManager(ChatRoom chatRoom, Member member) {

		if(isParticipantMember(chatRoom.getId(), member.getId())) {
			return;
		}

		if (chatRoom.canJoin()) {
			chatRoom.increaseParticipantCount();
		}


		ChatParticipant chatParticipant = ChatParticipant.builder()
			.chatRoom(chatRoom)
			.member(member)
			.roomRole(RoomRole.MANAGER)
			.build();
		chatParticipantRepository.save(chatParticipant);
	}

	@Transactional(readOnly = true)
	public boolean isParticipantMember(Long chatRoomId, Long memberId) {
		return chatParticipantRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId);
	}

	@Transactional(readOnly = true)
	public long countParticipantsInRoom(ChatRoom chatRoom) {
		return chatParticipantRepository.countByChatRoom(chatRoom);
	}
}
