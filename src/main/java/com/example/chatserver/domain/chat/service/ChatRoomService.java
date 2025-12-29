package com.example.chatserver.domain.chat.service;

import static com.example.chatserver.domain.chat.entity.QChatRoom.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.chatserver.domain.chat.dto.response.ChatRoomListResDto;
import com.example.chatserver.domain.chat.dto.response.MyChatListResDto;
import com.example.chatserver.domain.chat.dto.request.ChatRoomCreate;
import com.example.chatserver.domain.chat.dto.request.ChatRoomSearch;
import com.example.chatserver.domain.chat.entity.ChatParticipant;
import com.example.chatserver.domain.chat.entity.ChatRoom;
import com.example.chatserver.domain.chat.entity.QChatParticipant;
import com.example.chatserver.domain.chat.repository.ChatMessageRepository;
import com.example.chatserver.domain.chat.repository.ChatParticipantRepository;
import com.example.chatserver.domain.chat.repository.ChatRoomRepository;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.repository.MemberRepository;
import com.example.chatserver.global.common.paging.PageRequestDTO;
import com.example.chatserver.global.common.paging.PageResponseDTO;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
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
	private final ChatParticipantService chatParticipantService;
	private final ChatMessageRepository chatMessageRepository;
	private final JPAQueryFactory queryFactory;

	public String createGroupRoom(ChatRoomCreate chatRoomCreate, Member member) {

		// 채팅방 생성
		ChatRoom chatRoom = null;
		if (chatRoomCreate.isSecret()) {
			chatRoom = ChatRoom.builder()
				.name(chatRoomCreate.getRoomName())
				.ownerId(member.getId())
				.isSecret(true)
				.password(chatRoomCreate.getPassword())
				.isGroupChat(true)
				.maxParticipants(chatRoomCreate.getMaxParticipantCnt())
				.build();
		} else {
			chatRoom = ChatRoom.builder()
				.name(chatRoomCreate.getRoomName())
				.isSecret(false)
				.isGroupChat(true)
				.maxParticipants(chatRoomCreate.getMaxParticipantCnt())
				.build();
		}

		chatRoomRepository.save(chatRoom);
		chatParticipantService.addParticipantManager(chatRoom, member);
		return chatRoom.getRoomId();
	}

	/**
	 * 내 채팅방 목록 조회
	 *
	 * @return List<MyChatListResDto>
	 */
	public List<MyChatListResDto> getMyChatRooms(Member member) {

		// 1. member로 내가 참여한 chatParticipant 조회
		List<ChatParticipant> chatParticipants = chatParticipantService.getParticipantsByMember(member);

		// 2. 각 채팅방별로 읽지 않은 메시지 개수 조회 및 DTO 변환
		List<MyChatListResDto> chatListResDtos = new ArrayList<>();

		for (ChatParticipant c : chatParticipants) {
			// Long count = readStatusRepository.countByChatRoomAndMemberAndIsReadFalse(c.getChatRoom(), member);
			long unreadCount = chatMessageRepository.countByRoomIdAndIdGreaterThan(
				c.getChatRoom(),
				c.getLastReadMessageId() == null ? 0L : c.getLastReadMessageId()
			);
			MyChatListResDto dto = MyChatListResDto.builder()
				.roomId(c.getChatRoom().getRoomId())
				.roomName(c.getChatRoom().getName())
				.isGroupChat(c.getChatRoom().isGroupChat())
				.unReadCount(unreadCount)
				.lastMessage(
					c.getChatRoom().getLastMessageContent()
				)
				.lastMessageTime(
					c.getChatRoom().getLastMessageTime() != null ? c.getChatRoom().getLastMessageTime() : LocalDateTime.now()
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
			chatRoomRepository.delete(chatRoom);
		}



	}

	public String getOrCreatePrivateRoom(String otherMemberId) {
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
			return chatRoom.get().getRoomId();
		}
		// 만약 1:1 개인 채팅방이 없다면 기존 채팅방을 생성
		ChatRoom newRoom = ChatRoom.builder()
			.isGroupChat(false)
			.name(member.getName() + "-" + otherMember.getName())
			.isSecret(false)
			.password(null)
			.build();

		chatRoomRepository.save(newRoom);

		// 두사람 모두 참여자로 새롭게 추가
		chatParticipantService.addParticipantManager(newRoom, member);
		chatParticipantService.addParticipantMember(newRoom, otherMember);

		return newRoom.getRoomId();
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

		chatParticipantService.addParticipantMember(chatRoom, member);

	}


	/**
	 * 그룹 채팅방 목록 조회
	 *
	 * @param chatRoomSearch
	 * @return List<ChatRoomListResDto>
	 */
	public PageResponseDTO<ChatRoomListResDto> getGroupChatRooms(
		ChatRoomSearch chatRoomSearch,
		PageRequestDTO pageRequestDTO,
		Member currentMember) {

		Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getSize());
		QChatParticipant chatParticipant = QChatParticipant.chatParticipant;
		List<ChatRoomListResDto> content = queryFactory
			// 1. Projection: 엔티티 전체가 아니라 DTO에 필요한 필드만 조회 (성능 최적화)
			.select(Projections.fields(ChatRoomListResDto.class,
				chatRoom.roomId.as("roomId"),
				chatRoom.name.as("roomName"),
				chatRoom.lastMessageTime.as("lastMessageTime"),
				chatRoom.isSecret.as("isSecret"),
					// [핵심] 서브쿼리로 참여 여부 확인 (True/False 반환)
					ExpressionUtils.as(
						JPAExpressions.selectOne()
							.from(chatParticipant)
							.where(
								chatParticipant.chatRoom.eq(chatRoom),
								chatParticipant.member.eq(currentMember) // 혹은 member.id.eq(currentMember.getId())
							)
							.exists(),
						"isParticipated" // DTO의 필드명과 일치시켜야 함
					)
				)
			)
			.from(chatRoom)
			.where(
				// 2. 동적 쿼리: search 파라미터가 null이면 조건 무시, 있으면 조건 추가
				eqIsGroupChat(),
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
				eqIsGroupChat(),
				eqIsSecret(chatRoomSearch.getIsSecretChat()),
				eqIsParticipated(chatRoomSearch.getIsParticipated(), currentMember),
				containsName(chatRoomSearch.getRoomName())
			)
			.fetchOne();

		return new PageResponseDTO<>(content, totalCount, pageRequestDTO);
	}


	private BooleanExpression eqIsGroupChat() {
		return chatRoom.isGroupChat.eq(true);
	}

	private BooleanExpression eqIsSecret(Boolean isSecret) {
		if (isSecret == null)
			return null;
		return chatRoom.isSecret.eq(isSecret);
	}

	private BooleanExpression containsName(String name) {
		return StringUtils.hasText(name) ? chatRoom.name.contains(name) : null;
	}

	private BooleanExpression eqIsParticipated(Boolean isParticipated, Member currentMember) {
		if (isParticipated == null || !isParticipated) {
			return null; // 필터링 안 함 (전체 조회)
		}

		// 내가 참여한 방만 조회 (Join을 쓰는 게 서브쿼리보다 성능상 유리할 수 있음)
		// 하지만 위 코드 구조상 서브쿼리로 처리하는 게 깔끔함
		return JPAExpressions.selectOne()
			.from(QChatParticipant.chatParticipant)
			.where(
				QChatParticipant.chatParticipant.chatRoom.eq(chatRoom),
				QChatParticipant.chatParticipant.member.eq(currentMember)
			)
			.exists();
	}
}
