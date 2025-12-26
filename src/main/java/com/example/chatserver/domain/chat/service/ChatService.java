package com.example.chatserver.domain.chat.service;

import com.example.chatserver.domain.chat.entity.ChatMessage;
import com.example.chatserver.domain.chat.entity.ChatParticipant;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.chat.dto.ChatMessageDto;
import com.example.chatserver.domain.chat.entity.RoomRole;
import com.example.chatserver.domain.chat.repository.ChatMessageRepository;
import com.example.chatserver.domain.chat.repository.ChatParticipantRepository;
import com.example.chatserver.domain.chat.repository.ChatRoomRepository;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.repository.MemberRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final JPAQueryFactory queryFactory;
    private final ChatParticipantService chatParticipantService;

    public ChatService(ChatRoomRepository chatRoomRepository, ChatParticipantRepository chatParticipantRepository, ChatMessageRepository chatMessageRepository, MemberRepository memberRepository, JPAQueryFactory queryFactory,
        ChatParticipantService chatParticipantService) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.memberRepository = memberRepository;
        this.queryFactory = queryFactory;
        this.chatParticipantService = chatParticipantService;
    }


    public void saveMessage(String roomId, ChatMessageDto chatMessageDto) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
                () -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
        );
        // 보낸 사람 조회
        Member sender = memberRepository.findByEmail(chatMessageDto.getSenderEmail()).orElseThrow(
                () -> new EntityNotFoundException("보낸 사람을 찾을 수 없습니다.")
        );
        // 메시지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .member(sender)
                .content(chatMessageDto.getMessage())
                .build();
        ChatMessage newChatMessage = chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessage(newChatMessage);

        // Fetch Join으로 방 정보까지 한 번에 가져옴
        updateReadStatus(roomId, sender.getPublicId());
    }




    public void addParticipantToGroupChat(String roomId) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
                () -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
        );
        // member 조회
        Member member = memberRepository.findByPublicId(SecurityContextHolder.getContext()
                .getAuthentication().getName()).orElseThrow(
                () -> new EntityNotFoundException("사용자를 찾을 수 없습니다.")
        );

        if (!chatRoom.isGroupChat()) {
            throw new IllegalArgumentException("그룹채팅이 아닙니다.");
        }

        // 이미 참여자인지 검증
        Optional<ChatParticipant> participant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member);
        if(participant.isEmpty()){
            addParticipant(chatRoom, member);
        }


    }

    // chatParticipant 객체 생성 후 저장
    public void addParticipant(ChatRoom chatRoom, Member member) {
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .roomRole(RoomRole.NORMAL)
                .build();
        chatParticipantRepository.save(chatParticipant);
    }

    public List<ChatMessageDto> getChatHistory(String roomId) {
        // 내가 해당 채팅방의 참여자가 아닐 경우 에러
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(
                () -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
        );
        // 참여자 조회
        Member member = memberRepository.findByPublicId(SecurityContextHolder.getContext()
                .getAuthentication().getName()).orElseThrow(
                () -> new EntityNotFoundException("사용자를 찾을 수 없습니다.")
        );

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        boolean check = false;
        for (ChatParticipant c : chatParticipants) {
            if (c.getMember().equals(member)) {
                check = true;
            }
        }

        if(!check) throw new IllegalArgumentException("본인이 속하지 않은 채팅방입니다.");
        // 특정 room에 대한 message 조회
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomOrderByCreatedTimeAsc(chatRoom);
        List<ChatMessageDto> chatMessageDtos = new ArrayList<>();
        for (ChatMessage c : chatMessages) {
            ChatMessageDto chatMessageDto = ChatMessageDto.builder()
                    .message(c.getContent())
                    .senderEmail(c.getMember().getEmail())
                    .createdAt(c.getCreatedTime())
                    .build();
            chatMessageDtos.add(chatMessageDto);
        }

        return chatMessageDtos;


    }

    public boolean isRoomParticipant(String publicId, String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));
        Member member = memberRepository.findByPublicId(publicId).orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        for (ChatParticipant c : chatParticipants) {
            if(c.getMember().equals(member)) {
                return true;
            }
        }
        return false;
    }


    public void messageRead(String roomId) {
        String publicId = SecurityContextHolder.getContext().getAuthentication().getName();
        updateReadStatus(roomId, publicId);
    }

    private void updateReadStatus(String roomId, String publicId) {
        // Fetch Join으로 방 정보까지 한 번에 가져옴
        ChatParticipant participant = chatParticipantRepository.findByRoomIdAndMemberPublicId
				(roomId, publicId)
            .orElseThrow(
                () -> new EntityNotFoundException("참여방을 찾을 수 없습니다.")
            );

        // [최고 성능] ChatMessage 테이블 조회 없이(쿼리 0회), ChatRoom에 캐싱된 ID 사용
        Long latestId = participant.getChatRoom().getLastMessageId();

        if (latestId != null && (participant.getLastReadMessageId() == null || participant.getLastReadMessageId() < latestId)) {
            participant.updateLastReadMessage(latestId);
        }
    }

}
