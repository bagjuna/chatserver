package com.example.chatserver.domain.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class ChatRoomCreate {
	private String roomName;
	private int maxParticipantCnt;
	@JsonProperty("isSecret")
	private boolean isSecret;

	// optional
	private String password;
}
