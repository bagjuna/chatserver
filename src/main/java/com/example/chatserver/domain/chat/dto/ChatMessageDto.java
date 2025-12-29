package com.example.chatserver.domain.chat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    // 1. 메시지 타입 enum
    private MessageType messageType;
    // 2. 공통 메타 데이터
    private String roomId;
    private String senderEmail;
    private String senderName;
    private LocalDateTime createdAt;

    // [대화용] 실제 텍스트 내용
    private String message;

    // [이미지/파일용] S3 URL (나중에 확장 대비)
    private String fileUrl;

    // 읽음 처리 시 "마지막으로 읽은 메시지 ID"를 담을 필드
    // TALK일 때는 생성된 메시지 ID를 담아서 리턴용으로도 사용 가능
    private Long messageId;

    // [응답용] "이 메시지를 안 읽은 사람 수" (실시간 갱신용)
    private int unreadCount;

    public void updateMessageInfo(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void updateMessageId(Long messageId, int unreadCount) {
        this.messageId = messageId;
        this.unreadCount = unreadCount;
    }
}
