package com.example.chatserver.domain.chat.service;

import static com.example.chatserver.domain.chat.entity.QChatRoom.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.chatserver.domain.chat.dto.ChatRoomListResDto;
import com.example.chatserver.domain.chat.dto.MyChatListResDto;
import com.example.chatserver.domain.chat.dto.request.ChatRoomCreate;
import com.example.chatserver.domain.chat.dto.request.ChatRoomSearch;
import com.example.chatserver.domain.chat.entity.ChatMessage;
import com.example.chatserver.domain.chat.entity.ChatParticipant;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.chat.entity.RoomRole;
import com.example.chatserver.domain.chat.repository.ChatParticipantRepository;
import com.example.chatserver.domain.chat.repository.ChatRoomRepository;
import com.example.chatserver.domain.chat.repository.ReadStatusRepository;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.repository.MemberRepository;
import com.example.chatserver.global.common.paging.PageRequestDTO;
import com.example.chatserver.global.common.paging.PageResponseDTO;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomService {

	private final MemberRepository memberRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatParticipantRepository chatParticipantRepository;
	private final ReadStatusRepository readStatusRepository;
	private final JPAQueryFactory queryFactory;

	public String createGroupRoom(ChatRoomCreate chatRoomCreate, Member member) {

		// member 조회
		// String publicId = SecurityContextHolder.getContext().getAuthentication().getName();
		//
		// Member member = memberRepository.findByPublicId(publicId)
		// 	.orElseThrow(
		// 		() -> new EntityNotFoundException("사용자를 찾을 수 없습니다.")
		// 	);

		// 채팅방 생성
		ChatRoom chatRoom = null;
		if (chatRoomCreate.getIsSecretChat()) {
			chatRoom = ChatRoom.builder()
				.name(chatRoomCreate.getRoomName())
				.isSecret(true)
				.password(chatRoomCreate.getPassword())
				.isGroupChat(true)
				.build();
		} else {
			chatRoom = ChatRoom.builder()
				.name(chatRoomCreate.getRoomName())
				.isSecret(false)
				.isGroupChat(true)
				.build();
		}

		chatRoomRepository.save(chatRoom);
		// 채팅참여자로 개설자를 추가
		// ChatParticipant chatParticipant = ChatParticipant.builder()
		// 	.chatRoom(chatRoom)
		// 	.member(member)
		// 	.roomRole(RoomRole.MANAGER)
		// 	.build();
		//
		// chatParticipantRepository.save(chatParticipant);
		addParticipantManager(chatRoom, member);
		return chatRoom.getRoomId();
	}

	/**
	 * 내 채팅방 목록 조회
	 *
	 * @return List<MyChatListResDto>
	 */
	public List<MyChatListResDto> getMyChatRooms(Member member) {

		// 1. member로 내가 참여한 chatParticipant 조회
		List<ChatParticipant> chatParticipants = chatParticipantRepository.findAllByMember(member);

		// 2. 각 채팅방별로 읽지 않은 메시지 개수 조회 및 DTO 변환
		List<MyChatListResDto> chatListResDtos = new ArrayList<>();

		for (ChatParticipant c : chatParticipants) {
			Long count = readStatusRepository.countByChatRoomAndMemberAndIsReadFalse(c.getChatRoom(), member);
			int size = c.getChatRoom().getChatMessages().size();
			ChatMessage lastMessage = null;
			if (size > 0) {
				lastMessage = c.getChatRoom().getChatMessages().get(size - 1);

			}
			MyChatListResDto dto = MyChatListResDto.builder()
				.roomId(c.getChatRoom().getRoomId())
				.roomName(c.getChatRoom().getName())
				.isGroupChat(c.getChatRoom().isGroupChat())
				.unReadCount(count)
				.lastMessage(
					size == 0 ? null : lastMessage.getContent()
				)
				.lastMessageTime(
					size == 0 ? null : lastMessage.getCreatedTime()
				)
				.build();
			chatListResDtos.add(dto);
		}

		return chatListResDtos.stream().sorted(
			(o1, o2) -> {
				if (o1.getUnReadCount() != o2.getUnReadCount()) {
					return o2.getUnReadCount().compareTo(o1.getUnReadCount());
				}
				else {
					return o2.getLastMessageTime().compareTo(o1.getLastMessageTime());

				}

			}
		).collect(Collectors.toList());
	}

	public void leaveGroupChatRoom(String roomId) {
		ChatRoom chatRoom = chatRoomRepository.findById(Long.parseLong(roomId))
			.orElseThrow(() -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));
		Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
			.orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
		if (!chatRoom.isGroupChat()) {
			throw new IllegalArgumentException("단체 채팅방이 아닙니다.");
		}
		ChatParticipant c = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member)
			.orElseThrow(() -> new EntityNotFoundException("참여방을 찾을 수 없습니다."));
		chatParticipantRepository.delete(c);

		List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
		if (chatParticipants.isEmpty()) {

		}

	}

	public Long getOrCreatePrivateRoom(String otherMemberId) {
		Member member = memberRepository.findByPublicId(
			SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(
			() -> new EntityNotFoundException("사용자를 찾을 수 없습니다.")
		);

		Member otherMember = memberRepository.findByPublicId(otherMemberId).orElseThrow(
			() -> new EntityNotFoundException("상대방을 찾을 수 없습니다.")
		);

		Optional<ChatRoom> chatRoom = chatParticipantRepository.findChatRoomIdExistingPrivateRoom(member.getId(),
			otherMember.getId());
		if (chatRoom.isPresent()) {
			// 이미 존재하는 개인 채팅방이 있다면
			return chatRoom.get().getId();
		}
		// 만약 1:1 개인 채팅방이 없다면 기존 채팅방을 생성
		ChatRoom newRoom = ChatRoom.builder()
			.isGroupChat(false)
			.name(member.getName() + "-" + otherMember.getName())
			.isGroupChat(false)
			.isSecret(false)
			.password(null)
			.build();

		chatRoomRepository.save(newRoom);

		// 두사람 모두 참여자로 새롭게 추가
		addParticipantManager(newRoom, member);
		addParticipantMember(newRoom, otherMember);

		return newRoom.getId();
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

		if (chatRoom.isGroupChat()) {
			throw new IllegalArgumentException("그룹채팅이 아닙니다.");
		}

		// 이미 참여자인지 검증
		Optional<ChatParticipant> participant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member);
		if (!participant.isPresent()) {
			addParticipantMember(chatRoom, member);
		}

	}

	// chatRoom에 참여자 추가 (일반 멤버MEMBER)
	public void addParticipantMember(ChatRoom chatRoom, Member member) {
		ChatParticipant chatParticipant = ChatParticipant.builder()
			.chatRoom(chatRoom)
			.member(member)
			.roomRole(RoomRole.NORMAL)
			.build();
		chatParticipantRepository.save(chatParticipant);
	}

	// chatRoom에 참여자 추가 (매니저 역할)
	public void addParticipantManager(ChatRoom chatRoom, Member member) {
		ChatParticipant chatParticipant = ChatParticipant.builder()
			.chatRoom(chatRoom)
			.member(member)
			.roomRole(RoomRole.MANAGER)
			.build();
		chatParticipantRepository.save(chatParticipant);

	}

	/**
	 * 그룹 채팅방 목록 조회
	 *
	 * @param chatRoomSearch
	 * @return List<ChatRoomListResDto>
	 */
	public PageResponseDTO<ChatRoomListResDto> getGroupChatRooms(
		ChatRoomSearch chatRoomSearch,
		PageRequestDTO pageRequestDTO) {

		Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getSize());

		List<ChatRoomListResDto> content = queryFactory
			// 1. Projection: 엔티티 전체가 아니라 DTO에 필요한 필드만 조회 (성능 최적화)
			.select(Projections.fields(ChatRoomListResDto.class,
				chatRoom.id.as("roomId"),
				chatRoom.name.as("roomName")

			))
			.from(chatRoom)
			.where(
				// 2. 동적 쿼리: search 파라미터가 null이면 조건 무시, 있으면 조건 추가
				eqIsGroupChat(chatRoomSearch.getIsGroupChat()),
				eqIsSecret(chatRoomSearch.getIsSecretChat()),
				containsName(chatRoomSearch.getRoomName())
			)
			.offset(pageable.getOffset()) // 3. 페이징: offset 설정
			.limit(pageable.getPageSize()) // 4. 페이징: limit 설정
			.orderBy(chatRoom.updatedTime.desc()) // 5. 정렬: 생성일자 내림차순
			// 6. 최종 조회
			.fetch();

		// 3. 카운트 조회 (fetchCount 대신 select(chatRoom.count()) 사용)
		// 리스트 쿼리에는 정렬(orderBy)이 필요하지만, 카운트 쿼리에는 필요 없어서 성능상 이득입니다.
		Long totalCount = queryFactory
			.select(chatRoom.count()) // count(*)과 동일
			.from(chatRoom)
			.where(
				eqIsGroupChat(chatRoomSearch.getIsGroupChat()),
				eqIsSecret(chatRoomSearch.getIsSecretChat()),
				containsName(chatRoomSearch.getRoomName())
			)
			.fetchOne();

		return new PageResponseDTO<>(content, totalCount, pageRequestDTO);
		// List<ChatRoom> chatRooms = chatRoomRepository.findByIsGroupChat("Y");
		// List<ChatRoomListResDto> dtos = new ArrayList<>();
		// for (ChatRoom c : chatRooms) {
		// 	ChatRoomListResDto dto = ChatRoomListResDto
		// 		.builder()
		// 		.roomId(c.getId())
		// 		.roomName(c.getName())
		// 		.build();
		// 	dtos.add(dto);
		// }
		// return dtos;
	}

	private BooleanExpression isGroupChatEq(boolean isGroupChat) {
		return chatRoom.isGroupChat.eq(isGroupChat);
	}

	private BooleanExpression eqIsGroupChat(Boolean isGroupChat) {
		if (isGroupChat == null)
			return null;
		return chatRoom.isGroupChat.eq(isGroupChat);
	}

	private BooleanExpression eqIsSecret(Boolean isSecret) {
		if (isSecret == null)
			return null;
		return chatRoom.isSecret.eq(isSecret);
	}

	private BooleanExpression containsName(String name) {
		return StringUtils.hasText(name) ? chatRoom.name.contains(name) : null;
	}
}
