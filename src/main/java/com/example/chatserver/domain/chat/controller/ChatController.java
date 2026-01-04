package com.example.chatserver.domain.chat.controller;

import com.example.chatserver.domain.chat.dto.ChatMessageDto;
import com.example.chatserver.domain.chat.dto.ChatParticipantDto;
import com.example.chatserver.domain.chat.dto.SecretChatRoomJoinDto;
import com.example.chatserver.domain.chat.dto.request.ChatMessageSearch;
import com.example.chatserver.domain.chat.dto.request.ChatRoomSearch;
import com.example.chatserver.domain.chat.dto.response.ChatRoomListResDto;
import com.example.chatserver.domain.chat.service.ChatService;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.global.common.paging.PageRequestDTO;
import com.example.chatserver.global.common.paging.PageResponseDTO;
import com.example.chatserver.global.security.userdetails.CustomUserDetails;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;


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
    public ResponseEntity<?> getChatHistory(@PathVariable String roomId,
        @AuthenticationPrincipal UserDetails userDetails) {
        List<ChatMessageDto> chatMessageDtos = chatService.getChatHistory(roomId, userDetails.getUsername());
        return new ResponseEntity<>(chatMessageDtos, HttpStatus.OK);
    }

    @GetMapping("/history/{roomId}/paged")
    public ResponseEntity<PageResponseDTO<ChatMessageDto>> getChatHistoryPaged(
        @ModelAttribute ChatMessageSearch chatMessageSearch,
        // 예: /history/{roomId}/paged?
        @ModelAttribute PageRequestDTO pageRequestDTO,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Member member = userDetails.getMember();
        return new ResponseEntity<>(chatService.getChatHistoryPaged(chatMessageSearch, pageRequestDTO, member), HttpStatus.OK);
    }

    // 참여자 목록 조회
    @GetMapping("/room/{roomId}/participants")
    public ResponseEntity<?> getChatParticipants(@PathVariable String roomId) {
        List<ChatParticipantDto> chatParticipants = chatService.getChatParticipants(roomId);
        return new ResponseEntity<>(chatParticipants, HttpStatus.OK);
    }

    // 채팅메시지 읽음처리
    // @PostMapping("/room/{roomId}/read")
    // public ResponseEntity<?> readChatMessage(@PathVariable String roomId) {
    //     chatService.messageRead(roomId);
    //     return ResponseEntity.ok().build();
    // }


}
