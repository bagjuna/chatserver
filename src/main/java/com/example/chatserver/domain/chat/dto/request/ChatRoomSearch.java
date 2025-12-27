package com.example.chatserver.domain.chat.dto.request;

import lombok.Data;

@Data
public class ChatRoomSearch {
	private String roomName;
	private Boolean isParticipated;
	private Boolean isSecretChat;
}
