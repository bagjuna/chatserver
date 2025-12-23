package com.example.chatserver.domain.chat.repository;

import com.example.chatserver.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByIsGroupChat(Boolean isGroupChat);

	Optional<ChatRoom> findByRoomId(String roomId);
}
