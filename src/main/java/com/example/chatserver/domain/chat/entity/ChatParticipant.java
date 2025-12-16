package com.example.chatserver.domain.chat.entity;

import com.example.chatserver.global.common.BaseTimeEntity;
import com.example.chatserver.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


// 채팅방 참여자 Entity
// ChatRoom 과 Member 를 연결하는 다대다 관계의 중간 Entity
// 각 참여자의 역할(RoomRole) 정보를 추가로 저장
// 예: 일반 참여자, 관리자 등
// BaseTimeEntity 를 상속받아 생성 및 수정 시간을 자동으로 관리
// JPA 어노테이션을 사용하여 DB 테이블과 매핑
// 롬복 어노테이션을 사용하여 생성자, 빌더, 게터 메서드 자동 생성
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ChatParticipant extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    private RoomRole roomRole;
}
