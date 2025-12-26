package com.example.chatserver.domain.chat.repository;

import com.example.chatserver.domain.chat.entity.ChatParticipant;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);

    Optional<ChatParticipant> findByChatRoomAndMember(ChatRoom chatRoom, Member member);

    List<ChatParticipant> findAllByMember(Member member);

    @Query("SELECT cp FROM ChatParticipant cp " +
        "JOIN FETCH cp.chatRoom " +
        "WHERE cp.member = :member")
    List<ChatParticipant> findAllByMemberWithRoom(@Param("member") Member member);

    @Query("SELECT cp1.chatRoom FROM ChatParticipant cp1 JOIN ChatParticipant cp2 " +
            "ON cp1.chatRoom.id =cp2.chatRoom.id " +
            "WHERE cp1.member.id = :myId AND cp2.member.id = :otherMemberId " +
            "AND cp1.chatRoom.isGroupChat = false")
    Optional<ChatRoom> findChatRoomIdExistingPrivateRoom(@Param("myId") Long myId, @Param("otherMemberId") Long otherMemberId);

    @Query("SELECT cp FROM ChatParticipant cp " +
        "WHERE cp.chatRoom.roomId = :roomId AND cp.member.publicId = :publicId")
    Optional<ChatParticipant> findByRoomIdAndMemberPublicId(String roomId, String publicId);
}
