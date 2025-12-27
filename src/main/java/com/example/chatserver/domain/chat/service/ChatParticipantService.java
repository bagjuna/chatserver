package com.example.chatserver.domain.chat.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chatserver.domain.chat.entity.ChatMessage;
import com.example.chatserver.domain.chat.entity.ChatParticipant;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.chat.entity.RoomRole;
import com.example.chatserver.domain.chat.repository.ChatMessageRepository;
import com.example.chatserver.domain.chat.repository.ChatParticipantRepository;
import com.example.chatserver.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class ChatParticipantService {

	private final ChatParticipantRepository chatParticipantRepository;

	public void addParticipantMember(ChatRoom chatRoom, Member member) {
		ChatParticipant chatParticipant = ChatParticipant.builder()
			.chatRoom(chatRoom)
			.member(member)
			.roomRole(RoomRole.NORMAL)
			.build();
		chatParticipantRepository.save(chatParticipant);
	}

	public void addParticipantManager(ChatRoom chatRoom, Member member) {
		ChatParticipant chatParticipant = ChatParticipant.builder()
			.chatRoom(chatRoom)
			.member(member)
			.roomRole(RoomRole.MANAGER)
			.build();
		chatParticipantRepository.save(chatParticipant);
	}

}
