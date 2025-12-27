package com.example.chatserver.domain.chat.dto.response;

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
    private String roomId;                  // 채팅방 ID
    private String roomName;               // 채팅방 이름
    private boolean isGroupChat;           // 그룹 채팅 여부
    private Long unReadCount;              // 읽지 않은 메시지 수
    private String lastMessage;            // 마지막 메시지 내용
    private LocalDateTime lastMessageTime; // 마지막 메시지 시간
}
