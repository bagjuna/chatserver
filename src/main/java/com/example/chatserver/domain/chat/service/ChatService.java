package com.example.chatserver.domain.chat.service;

import com.example.chatserver.domain.chat.dto.request.ChatRoomSearch;
import com.example.chatserver.domain.chat.entity.ChatMessage;
import com.example.chatserver.domain.chat.entity.ChatParticipant;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.chat.entity.QChatRoom;
import com.example.chatserver.domain.chat.entity.ReadStatus;
import com.example.chatserver.domain.chat.dto.ChatMessageDto;
import com.example.chatserver.domain.chat.dto.ChatRoomListResDto;
import com.example.chatserver.domain.chat.dto.MyChatListResDto;
import com.example.chatserver.domain.chat.repository.ChatMessageRepository;
import com.example.chatserver.domain.chat.repository.ChatParticipantRepository;
import com.example.chatserver.domain.chat.repository.ChatRoomRepository;
import com.example.chatserver.domain.chat.repository.ReadStatusRepository;
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
    private final ReadStatusRepository readStatusRepository;
    private final MemberRepository memberRepository;
    private final JPAQueryFactory queryFactory;

    public ChatService(ChatRoomRepository chatRoomRepository, ChatParticipantRepository chatParticipantRepository, ChatMessageRepository chatMessageRepository, ReadStatusRepository readStatusRepository, MemberRepository memberRepository, JPAQueryFactory queryFactory) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.readStatusRepository = readStatusRepository;
        this.memberRepository = memberRepository;
        this.queryFactory = queryFactory;
    }


    public void saveMessage(Long roomId, ChatMessageDto chatMessageDto) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
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
        chatMessageRepository.save(chatMessage);
        // 사용자별로 읽음 상태 저장
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        for (ChatParticipant c : chatParticipants) {
            ReadStatus readStatus = ReadStatus.builder()
                    .chatRoom(c.getChatRoom())
                    .member(c.getMember())
                    .chatMessage(chatMessage)
                    .isRead(c.getMember().equals(sender))
                    .build();
            readStatusRepository.save(readStatus);
        }



    }




    public void addParticipantToGroupChat(String roomId) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(Long.parseLong(roomId)).orElseThrow(
                () -> new EntityNotFoundException("채팅방이 존재하지 않습니다.")
        );
        // member 조회
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext()
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
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext()
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
                    .build();
            chatMessageDtos.add(chatMessageDto);
        }

        return chatMessageDtos;


    }

    public boolean isRoomParticipant(String email, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        for (ChatParticipant c : chatParticipants) {
            if(c.getMember().equals(member)) {
                return true;
            }
        }
        return false;
    }


    public void messageRead(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        List<ReadStatus> readStatuses = readStatusRepository.findByChatRoomAndMember(chatRoom, member);
        for(ReadStatus r : readStatuses) {
            r.updateIsRead(true);
        }
    }





}
