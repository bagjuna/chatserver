package com.example.chatserver.domain.chat.repository;

import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.chat.entity.ReadStatus;
import com.example.chatserver.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {

    List<ReadStatus> findByChatRoomAndMember(ChatRoom chatRoom, Member member);

    Long countByChatRoomAndMemberAndIsReadFalse(ChatRoom chatRoom, Member member);

}
