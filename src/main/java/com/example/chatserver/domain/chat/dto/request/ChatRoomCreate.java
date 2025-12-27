package com.example.chatserver.domain.chat.dto.request;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChatRoomCreate {
	private String roomName;
	private int maxParticipantCnt;
	private boolean isSecret;

	// optional
	private String password;
}
