package com.example.chatserver.domain.chat.dto;

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

}
