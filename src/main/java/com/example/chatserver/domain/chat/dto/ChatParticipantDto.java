package com.example.chatserver.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipantDto {
    private String email;
    private String name;
    private Long lastReadMessageId; // ★ 이 필드가 프론트로 넘어가야 함
}
