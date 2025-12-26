package com.example.chatserver.domain.chat.entity;

import com.example.chatserver.global.common.BaseTimeEntity;
import com.example.chatserver.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
//
// @Entity
// @NoArgsConstructor
// @AllArgsConstructor
// @Builder
// @Getter
// public class ReadStatus extends BaseTimeEntity {
//
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "chat_room_id", nullable = false)
//     private ChatRoom chatRoom;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "member_id", nullable = false)
//     private Member member;
//
//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "chat_message_id", nullable = false)
//     private ChatMessage chatMessage;
//
//     @Column(nullable = false)
//     private Boolean isRead;
//
//     public void updateIsRead(Boolean isRead) {
//         this.isRead = isRead;
//     }
//
// }
