package com.example.chatserver.domain.chat.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.chatserver.domain.chat.dto.ChatRoomListResDto;
import com.example.chatserver.domain.chat.dto.MyChatListResDto;
import com.example.chatserver.domain.chat.dto.request.ChatRoomCreate;
import com.example.chatserver.domain.chat.dto.request.ChatRoomSearch;
import com.example.chatserver.domain.chat.service.ChatRoomService;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.global.common.paging.PageRequestDTO;
import com.example.chatserver.global.common.paging.PageResponseDTO;
import com.example.chatserver.global.security.userdetails.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat/room")
@RequiredArgsConstructor
public class ChatRoomController {

	private final ChatRoomService chatRoomService;

	// 그룹 채팅방 개설
	@PostMapping("/group/create")
	public ResponseEntity<?> createGroupChatRoom(@RequestBody ChatRoomCreate chatRoomCreate,
		@AuthenticationPrincipal CustomUserDetails userDetails) {
		Member member = userDetails.getMember();
		String groupRoomId = chatRoomService.createGroupRoom(chatRoomCreate, member);
		return ResponseEntity.ok().body(groupRoomId);
	}

	// 그룹채팅목록조회
	@GetMapping("/group/list")
	public ResponseEntity<PageResponseDTO<ChatRoomListResDto>> getGroupChatRoomList(
		@ModelAttribute ChatRoomSearch chatRoomSearch,
		// 예: /group/list?page=1&size=10&roomName=축구
		@ModelAttribute PageRequestDTO pageRequestDTO
	) {

		return new ResponseEntity<>(chatRoomService.getGroupChatRooms(chatRoomSearch, pageRequestDTO), HttpStatus.OK);
	}


	// 내 채팅방 목록 조회 : roomId, roomName, 그룹채팅여부, 메시지읽음개수
	@GetMapping("/my/rooms")
	public ResponseEntity<?> getMyChatRoomList(@AuthenticationPrincipal CustomUserDetails userDetails) {
		List<MyChatListResDto> myChatRoomList = chatRoomService.getMyChatRooms(
			);
		return new ResponseEntity<>(myChatRoomList, HttpStatus.OK);
	}


	// 채팅방 나가기
	@DeleteMapping("/group/{roomId}/leave")
	public ResponseEntity<?> leaveGroupChatRoom(@PathVariable String roomId) {
		chatRoomService.leaveGroupChatRoom(roomId);
		return ResponseEntity.ok().build();
	}


	// 개인 채팅방 개설 또는 roomId return
	@PostMapping("/private/create")
	public ResponseEntity<?> createPrivateChatRoom(@RequestParam String otherMemberPublicId) {
		Long roomId = chatRoomService.getOrCreatePrivateRoom(otherMemberPublicId);
		return new ResponseEntity<>(roomId, HttpStatus.OK);
	}



}
