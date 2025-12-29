package com.example.chatserver.domain.chat.controller;

import java.time.LocalDateTime;

import com.example.chatserver.domain.chat.dto.ChatMessageDto;
import com.example.chatserver.domain.chat.dto.MessageType;
import com.example.chatserver.domain.chat.service.ChatService;
import com.example.chatserver.domain.chat.service.RedisPubSubService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class StompController {

    private final SimpMessageSendingOperations messageTemplate;

    private final ChatService chatService;
    private final RedisPubSubService pubSubService;
    private final ObjectMapper objectMapper; // [수정] 여기서 주입 받음 (final 필수)





    // MessageMapping 어노테이션만 활용
    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessageDto chatMessageDto) throws JsonProcessingException {
        // DTO에 방 번호 세팅
        chatMessageDto.setRoomId(roomId);

        // 1. 메시지 타입별 분기 처리
        if (MessageType.ENTER.equals(chatMessageDto.getMessageType())) {
            // 입장 시: "OO님이 입장하셨습니다" 메시지 생성 및 저장
            chatMessageDto.setMessage(chatMessageDto.getSenderName() + "님이 입장하셨습니다.");
            chatMessageDto.setCreatedAt(LocalDateTime.now());
            chatService.saveMessage(roomId, chatMessageDto);
        }
        else if (MessageType.TALK.equals(chatMessageDto.getMessageType())) {
            // 일반 대화: DB 저장 및 lastMessage 갱신
            chatMessageDto.setCreatedAt(LocalDateTime.now());
            chatService.saveMessage(roomId, chatMessageDto);
        }
        else if (MessageType.READ.equals(chatMessageDto.getMessageType())) {
            // 읽음 처리 후, '실제로 업데이트된 마지막 메시지 ID'를 반환받습니다.
            chatMessageDto.setCreatedAt(LocalDateTime.now());
            chatService.messageRead(roomId, chatMessageDto);

            // READ 타입은 메시지 내용(content)이 필요 없으므로 null 처리해도 됩니다.
            chatMessageDto.setMessage(null);
        }

        // 2. 구독자들에게 전송
        // messageTemplate.convertAndSend("/topic/" + roomId, chatMessageDto);
        String message = objectMapper.writeValueAsString(chatMessageDto);
        pubSubService.publish("chat", message);
    }


}
