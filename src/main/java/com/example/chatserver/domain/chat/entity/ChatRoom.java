package com.example.chatserver.domain.chat.entity;


import com.example.chatserver.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "chat_room", indexes = {
    @Index(name = "idx_chat_room_uuid", columnList = "room_id") // roomId 에 유니크 인덱스 생성
})
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    // [외부용] API 요청/응답, URL 경로용 (유니크 인덱스 필수!)
    @Column(nullable = false, unique = true) // unique index 생성
    private String roomId;


    @PrePersist
    public void generateRoomId() {
        if (this.roomId == null) {
            this.roomId = UUID.randomUUID().toString();
        }
    }

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isGroupChat;

    @Column(nullable = false)
    private boolean isSecret;

    // 비밀번호는 암호화해서 저장 [추후 구현]
    private String password;

    private Long ownerId;

    // 최대 참여자 수
    private int maxParticipants;
    // 낙관적 락을 위한 버전 필드
    @Version
    private Long version;

    @Column(columnDefinition = "bigint default 0")
    private Long totalMessageCount = 0L;

    //== 반정규화 필드 (목록 조회용)==//
    private Long lastMessageId;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE)
    private List<ChatParticipant> chatParticipants = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();


    @Builder
    public ChatRoom(String name, boolean isGroupChat, boolean isSecret, String password, int maxParticipants, Long ownerId) {
        this.roomId = UUID.randomUUID().toString();
        this.name = name;
        this.isGroupChat = isGroupChat;
        this.isSecret = isSecret;
        this.password = password;
        this.maxParticipants = maxParticipants;
        this.ownerId = ownerId;
        this.totalMessageCount = 0L;
    }

    // 인원수 확인 로직 (서비스 계층에서 호출)
    public boolean canJoin() {
        return this.chatParticipants.size() < this.maxParticipants;
    }

    // 메시지 보낼 때마다 이 메서드 호출해서 갱신
    public void updateLastMessage(ChatMessage chatMessage) {
        this.lastMessageId = chatMessage.getId();
        this.lastMessageContent = chatMessage.getContent();
        this.lastMessageTime = chatMessage.getCreatedTime();
        this.totalMessageCount++;
    }

}
