package com.example.chatserver.domain.chat.dto.request;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChatRoomCreate {
	private String roomName;
	private Boolean isGroupChat;
	private Boolean isSecretChat;

	// optional
	private String password;
}
