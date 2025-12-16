package com.example.chatserver.domain.chat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MyChatListResDto {
    private String roomId;
    private String roomName;
    private boolean isGroupChat;
    private Long unReadCount;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
}
