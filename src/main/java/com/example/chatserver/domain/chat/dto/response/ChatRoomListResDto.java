package com.example.chatserver.domain.chat.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomListResDto {
    private String roomId;
    private String roomName;
    private LocalDateTime lastMessageTime;
    private Boolean isSecret;
    private Boolean isParticipated;

}
