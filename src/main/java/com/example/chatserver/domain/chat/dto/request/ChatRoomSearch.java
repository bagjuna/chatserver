package com.example.chatserver.domain.chat.dto.request;

import lombok.Data;

@Data
public class ChatRoomSearch {
	private String roomName;
	private Boolean isGroupChat;
	private Boolean isSecretChat;
}
