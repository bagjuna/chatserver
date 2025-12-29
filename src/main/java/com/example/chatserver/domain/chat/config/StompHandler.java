package com.example.chatserver.domain.chat.config;


import java.security.Principal;
import java.util.Map;

import javax.crypto.SecretKey;

import com.example.chatserver.domain.chat.service.ChatService;
import com.example.chatserver.global.common.error.BaseException;
import com.example.chatserver.global.common.error.ErrorCode;
import com.example.chatserver.global.security.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

	private static final String SESSION_USER_KEY = "USER_PRINCIPAL";

	private final UserDetailsService userDetailsService;
	private final JwtUtil jwtUtil;
	private final ChatService chatService;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

		// 1. 연결 요청 (CONNECT)
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String token = extractToken(accessor);

			// 토큰 검증
			jwtUtil.validateToken(token);

			// 유저 정보 조회 및 Principal 설정
			String userId = jwtUtil.getUserIdFromToken(token);
			UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

			UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

			// [핵심 1] Principal 설정 (현재 메시지용)
			// accessor.setUser(auth);

			// [핵심 2] 세션 속성(SessionAttributes)에 인증 객체 저장 (영구 저장용)
			// 이렇게 해야 SUBSCRIBE, SEND 등 다음 요청에서도 유저 정보를 찾을 수 있습니다.
			accessor.getSessionAttributes().put(SESSION_USER_KEY, auth);

			log.info("STOMP CONNECT Success: {}", userId);
		}

		// 2. 구독(SUBSCRIBE) 또는 전송(SEND) 요청 시 권한 검증
		else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
			|| StompCommand.SEND.equals(accessor.getCommand())) {
			try {
				// [핵심 3] 유저 정보 가져오기 (accessor.getUser()가 없으면 세션에서 찾기)
				// Principal user = accessor.getUser();
				//
				// if (user == null) {
				// 	Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
				// 	if (sessionAttributes != null) {
				// 		user = (Principal) sessionAttributes.get("USER_AUTH");
				// 	}
				// }
				//
				// // 여전히 유저가 없으면 에러
				// if (user == null) {
				// 	log.error("권한 검사 실패: 인증 정보가 없습니다.");
				// 	throw new BaseException(ErrorCode.JWT_AUTHENTICATION_FAIL);
				// }
				//
				// // (선택 사항) 다음 필터들을 위해 accessor에 다시 세팅해줄 수도 있음
				// accessor.setUser(user);
				Authentication auth = (Authentication) accessor.getSessionAttributes().get(SESSION_USER_KEY);
				// 목적지(Destination) 확인
				String destination = accessor.getDestination();

				// 목적지가 있고 채팅 관련 토픽인 경우 권한 검사 수행
				if (destination != null && destination.contains("/topic/")) {
					validateChatRoomPermission(auth, destination, accessor.getCommand());
				}
			} catch (Exception e) {
				log.error("SUBSCRIBE/SEND 권한 검사 실패: {}", e.getMessage());
				// 여기서 예외를 던져야 클라이언트 연결이 끊기거나 에러 프레임을 받습니다.
				// 만약 연결을 유지하고 싶다면 로그만 찍고 return message; (상황에 따라 다름)
				throw e;
			}
		}
		return message;
	}

	private String extractToken(StompHeaderAccessor accessor) {
		String bearerToken = accessor.getFirstNativeHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		// 토큰이 없으면 즉시 예외 발생
		log.error("CONNECT 실패: 토큰이 없습니다.");
		throw new BaseException(ErrorCode.JWT_TOKEN_MISSING);
	}

	/**
	 * 채팅방 참여 권한 검증 (읽기/쓰기 공통)
	 */
	private void validateChatRoomPermission(Principal user, String destination, StompCommand command) {
		try {
			// Destination 파싱: /topic/{roomId} 또는 /pub/chat/{roomId} 등
			// "/"로 잘라서 마지막 부분이 roomId라고 가정 (프로젝트 URL 설계에 따름)
			String roomId = destination.substring(destination.lastIndexOf("/") + 1);

			String publicId = user.getName();

			// DB나 캐시를 통해 실제 참여자인지 확인
			if (!chatService.isRoomParticipant(publicId, roomId)) {
				log.warn("{} 거부: 사용자({})는 방({})의 참여자가 아닙니다.", command, publicId, roomId);
				throw new BaseException(ErrorCode.ACCESS_DENIED);
			}
		} catch (Exception e) {
			log.error("권한 검사 중 오류 발생: {}", e.getMessage());
			throw new BaseException(ErrorCode.ACCESS_DENIED);
		}
	}
}
