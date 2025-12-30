package com.example.chatserver.domain.chat.dto.response;

import java.util.List;

import com.example.chatserver.domain.chat.dto.ChatParticipantDto;
import com.example.chatserver.domain.chat.dto.ChatRoomDetailDto;

import lombok.Builder;
import lombok.Data;

// DTO: 방 입장 시 필요한 모든 정보를 담는 껍데기
@Data
@Builder
public class ChatRoomEnterResponse {
	private ChatRoomDetailDto roomInfo;       // 방 정보 (이름, 비밀방 여부 등)
	private List<ChatParticipantDto> participants; // 참여자 목록
}
