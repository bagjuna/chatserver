package com.example.chatserver.chat.controller;

import com.example.chatserver.chat.dto.ChatMessageDto;
import com.example.chatserver.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class StompController {

    private final SimpMessageSendingOperations messageTemplate;

    private final ChatService chatService;

    public StompController(SimpMessageSendingOperations messageTemplate, ChatService chatService) {
        this.messageTemplate = messageTemplate;
        this.chatService = chatService;
    }

    // 방법 1: MessageMapping(수신)과 SendTo(topic에 메시지전달)를 사용하여 메시지 처리
//    @MessageMapping("/{roomId}") // 클라이언트에서 특정 publish/roomId 형태로 메시지를 발행시 MessageMapping으로 수신
//    @SendTo("/topic/{roomId}")  // 해당 roomId에 메시지를 발행하여 구독중인 클라이언트에게 전달
//    // DestinationVariable : @MessageMapping 어노테이션으로 정의된 WebSocket Controller 내에서만 사용 가능
//    public String sendMessage(@DestinationVariable Long roomId, String message) {
//        // 메시지 처리 로직
//        System.out.println(message);
//        return message;
//    }
//
    // 방법 2: MessageMapping 어노테이션만 활용
    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto chatMessageDto) {
        System.out.println(chatMessageDto.getMessage());
        chatService.saveMessage(roomId, chatMessageDto);
        messageTemplate.convertAndSend("/topic/" + roomId, chatMessageDto);
    }


}
