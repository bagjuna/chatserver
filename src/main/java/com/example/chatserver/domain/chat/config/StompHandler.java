package com.example.chatserver.domain.chat.config;


import java.util.Map;

import javax.crypto.SecretKey;

import com.example.chatserver.domain.chat.service.ChatService;
import com.example.chatserver.global.common.error.BaseException;
import com.example.chatserver.global.common.error.ErrorCode;
import com.example.chatserver.global.security.jwt.JwtUtil;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
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
public class StompHandler implements ChannelInterceptor {

	@Value("${jwt.secretKey}")
	private String SECRET_KEY;
	private SecretKey key;

	private final UserDetailsService userDetailsService;
	private final JwtUtil jwtUtil;

	private final ChatService chatService;

	public StompHandler(ChatService chatService, UserDetailsService userDetailsService, JwtUtil jwtUtil) {
		this.userDetailsService = userDetailsService;
		this.jwtUtil = jwtUtil;
		this.chatService = chatService;
	}

	@PostConstruct
	private void init() {
		this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		// 1. 연결 요청 (CONNECT) 일 때만 토큰을 검증합니다.
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {

			String bearerToken = accessor.getFirstNativeHeader("Authorization");

			// 토큰이 없으면 예외 발생
			if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
				log.error("CONNECT 실패: 토큰이 없습니다.");
				throw new BaseException(ErrorCode.JWT_TOKEN_MISSING);
			}

			String token = bearerToken.substring(7);

			try {
				// 토큰 검증
				jwtUtil.validateToken(token); // 유효하지 않으면 예외 발생

				// 유저 정보 추출 및 인증 객체 생성
				String userId = jwtUtil.getUserIdFromToken(token);
				UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

				UsernamePasswordAuthenticationToken auth =
					new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
				// [중요 1] Principal 설정
				accessor.setUser(auth);

				// [중요 2] 세션 속성에도 강제로 저장 (이중 안전장치)
				Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
				if (sessionAttributes != null) {
					sessionAttributes.put("CS_USER", auth);
				}

				log.info("STOMP CONNECT 인증 성공: {}", userId);

			} catch (Exception e) {
				log.error("STOMP CONNECT 인증 실패: {}", e.getMessage());
				throw new BaseException(ErrorCode.JWT_AUTHENTICATION_FAIL);
			}
		}

		// 2. 구독 요청 (SUBSCRIBE) 일 때 -> 세션에 저장된 인증 정보를 확인
		// 2. SUBSCRIBE: 인증 정보 확인 및 권한 검사
		else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

			// [중요 3] accessor.getUser()가 null이면 세션에서 가져오기
			Authentication auth = (Authentication) accessor.getUser();
			if (auth == null) {
				Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
				if (sessionAttributes != null) {
					auth = (Authentication) sessionAttributes.get("CS_USER");
				}
			}

			if (auth == null) {
				log.error("SUBSCRIBE 실패: 인증 정보가 없습니다.");
				throw new BaseException(ErrorCode.JWT_AUTHENTICATION_FAIL);
			}

			String email = ((UserDetails) auth.getPrincipal()).getUsername();
			String destination = accessor.getDestination();

			log.info("SUBSCRIBE 요청: {} -> {}", email, destination);

			// 채팅방 권한 검사
			if (destination != null && destination.contains("/topic/")) {
				try {
					String[] parts = destination.split("/");
					// /topic/{roomId} 형태라고 가정 (parts[0]="", parts[1]="topic", parts[2]="roomId")
					if (parts.length > 2) {
						String roomId = parts[2];
						if (!chatService.isRoomParticipant(email, roomId)) {
							log.warn("SUBSCRIBE 거부: {}는 방 {}의 참여자가 아닙니다.", email, roomId);
							throw new BaseException(ErrorCode.ACCESS_DENIED);
						}
					}
				} catch (NumberFormatException e) {
					// roomId 파싱 실패 시 무시 (유효하지 않은 토픽일 수 있음)
				}
			}
		}
		else if (StompCommand.MESSAGE.equals(accessor.getCommand())) {
			System.out.println("Message 검증");
		}
		else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
			System.out.println("DISCONNECT 요청");
		}

		return message;
	}

}
