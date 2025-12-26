package com.example.chatserver.domain.chat.repository;

import com.example.chatserver.domain.chat.entity.ChatMessage;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoomOrderByCreatedTimeAsc(ChatRoom chatRoom);

	@Query(
		"SELECT cm FROM ChatMessage cm WHERE cm.chatRoom = :chatRoom AND cm.id > :lastMessageId ORDER BY cm.createdTime ASC"
	)
	Optional<ChatMessage> findTopByChatRoomOrderByIdDesc(ChatRoom chatRoom);

	@Query("SELECT max(cm.id) FROM ChatMessage cm WHERE cm.chatRoom.roomId = :roomId")
	Long findLatestMessageIdByRoomId(@Param("roomId") String roomId);

	// long countByRoomIdAndIdGreaterThan(ChatRoom chatRoom, long l);
	@Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatRoom = :chatRoom AND cm.id > :lastReadMessageId")
	long countByRoomIdAndIdGreaterThan(ChatRoom chatRoom, long lastReadMessageId);
}
