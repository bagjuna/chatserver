package com.example.chatserver.domain.chat.config;


import javax.crypto.SecretKey;

import com.example.chatserver.domain.chat.service.ChatService;
import com.example.chatserver.global.common.error.BaseException;
import com.example.chatserver.global.common.error.ErrorCode;
import com.example.chatserver.global.security.jwt.JwtUtil;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

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

		String bearerToken = accessor.getFirstNativeHeader("Authorization");
		if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
			throw new BaseException(ErrorCode.JWT_TOKEN_MISSING);
		}

		String token = bearerToken.substring(7);


		try {
			jwtUtil.validateToken(token);
		} catch (Exception e) {
			throw new BaseException(ErrorCode.JWT_AUTHENTICATION_FAIL);
		}

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			System.out.println("connect 요청시 토큰 유효성 검증");
			System.out.println("토큰 검증 완료");
		}
		else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			System.out.println("Subscribe 검증");

			String userId = jwtUtil.getUserIdFromToken(token);
			UserDetails userDetails = userDetailsService.loadUserByUsername(userId); // throws UsernameNotFoundException
			String email = userDetails.getUsername();

			String roomId = accessor.getDestination().split("/")[2];
			if (!chatService.isRoomParticipant(email, Long.parseLong(roomId))) {
				throw new AuthenticationServiceException("해당 room에 권한이 없습니다.");
			}

		} else if (StompCommand.MESSAGE.equals(accessor.getCommand())) {
			System.out.println("Message 검증");
		}else if(StompCommand.DISCONNECT.equals(accessor.getCommand())) {
			System.out.println("DISCONNECT 요청");
		}
		else  if (StompCommand.SEND.equals(accessor.getCommand())) {
			System.out.println("SEND 검증");
		}

		return message;
	}

}
