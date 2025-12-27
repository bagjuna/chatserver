package com.example.chatserver.domain.chat.controller;

import com.example.chatserver.domain.chat.dto.ChatMessageDto;
import com.example.chatserver.domain.chat.dto.SecretChatRoomJoinDto;
import com.example.chatserver.domain.chat.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    // 그룹채팅방 참여
    @PostMapping("/room/group/{roomId}/join")
    public ResponseEntity<?> joinGroupChatRoom(@PathVariable String roomId) {
        chatService.addParticipantToGroupChat(roomId);
        return ResponseEntity.ok().build();
    }

    // 비밀 채팅방 참여
    @PostMapping("/room/secret/{roomId}/join")
    public ResponseEntity<?> joinPrivateChatRoom(@PathVariable String roomId,
        @RequestBody SecretChatRoomJoinDto request,
        @AuthenticationPrincipal UserDetails userDetails) {
        chatService.addParticipantToPrivateChat(roomId, request.getPassword(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }


    // 이전 메시지 조회
    @GetMapping("/history/{roomId}")
    public ResponseEntity<?> getChatHistory(@PathVariable String roomId) {
        List<ChatMessageDto> chatMessageDtos = chatService.getChatHistory(roomId);
        return new ResponseEntity<>(chatMessageDtos, HttpStatus.OK);
    }

    // 채팅메시지 읽음처리
    @PostMapping("/room/{roomId}/read")
    public ResponseEntity<?> readChatMessage(@PathVariable String roomId) {
        chatService.messageRead(roomId);
        return ResponseEntity.ok().build();
    }


}
