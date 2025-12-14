package com.example.chatserver.domain.chat.entity;


import com.example.chatserver.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
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

    // String "N" 대신 boolean 사용
    @Column(nullable = false)
    private boolean isGroupChat;

    @Column(nullable = false)
    private boolean isSecret;

    // 비밀번호는 암호화해서 저장하기
    private String password;


    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE)
    private List<ChatParticipant> chatParticipants = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();

    @Builder
    private ChatRoom(String name, boolean isGroupChat, boolean isSecret, String password) {
        this.name = name;
        this.isGroupChat = isGroupChat;
        this.isSecret = isSecret;
        this.password = password;
    }

}
