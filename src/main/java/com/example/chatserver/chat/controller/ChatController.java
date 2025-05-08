package com.example.chatserver.chat.controller;

import com.example.chatserver.chat.dto.ChatMessageDto;
import com.example.chatserver.chat.dto.ChatRoomListResDto;
import com.example.chatserver.chat.dto.MyChatListResDto;
import com.example.chatserver.chat.service.ChatService;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // 그룹 채팅방 개설
    @PostMapping("/room/group/create")
    public ResponseEntity<?> createGroupChatRoom(@RequestParam String roomName) {
        chatService.createGroupRoom(roomName);
        return ResponseEntity.ok().build();
    }

    // 그룹채팅목록조회
    @GetMapping("/room/group/list")
    public ResponseEntity<?> getGroupChatRoomList() {
        List<ChatRoomListResDto> chatRooms = chatService.getGroupChatRooms();
        return new ResponseEntity<>(chatRooms, HttpStatus.OK);
    }

    // 그룹채팅방 참여
    @PostMapping("/room/group/{roomId}/join")
    public ResponseEntity<?> joinGroupChatRoom(@PathVariable String roomId) {
        chatService.addParticipantToGroupChat(roomId);
        return ResponseEntity.ok().build();
    }

    // 이전 메시지 조회
    @GetMapping("/history/{roomId}")
    public ResponseEntity<?> getChatHistory(@PathVariable String roomId) {
        List<ChatMessageDto> chatMessageDtos = chatService.getChatHistory(roomId);
        return new ResponseEntity<>(chatMessageDtos, HttpStatus.OK);
    }

    // 채팅메시지 읽음처리
    @PostMapping("/read/{roomId}")
    public ResponseEntity<?> readChatMessage(@PathVariable String roomId) {
        chatService.messageRead(Long.parseLong(roomId));
        return ResponseEntity.ok().build();
    }

    // 내 채팅방 목록 조회 : roomId, roomName, 그룹채팅여부, 메시지읽음개수
    @GetMapping("/my/rooms")
    public ResponseEntity<?> getMyChatRoomList() {
        List<MyChatListResDto> myChatRoomList = chatService.getMyChatRooms();
        return new ResponseEntity<>(null, HttpStatus.OK);
    }


    // 채팅방 나가기
    @DeleteMapping("/room/group/{roomId}/leave")
    public ResponseEntity<?> leaveGroupChatRoom(@PathVariable String roomId) {
        chatService.leaveGroupChatRoom(roomId);
        return ResponseEntity.ok().build();
    }


}
