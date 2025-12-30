package com.example.chatserver.domain.chat.dto.request;

import com.example.chatserver.domain.chat.dto.MessageType;

import lombok.Data;

@Data
public class ChatMessageSearch {
	private String messageContent;
	private MessageType messageType;
}
