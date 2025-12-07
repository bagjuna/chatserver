package com.example.chatserver.domain.chat.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompHandler stompHandler;

    public StompWebSocketConfig(StompHandler stompHandler) {
        this.stompHandler = stompHandler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트에서 /ws로 연결 요청이 들어오면, 이 endpoint로 연결됨
        registry.addEndpoint("/connect")
                // cors 설정
                .setAllowedOrigins("http://localhost:3000")
                // ws://가 아닌 http://로 연결 요청이 들어올 경우, SockJS를 사용하여 연결
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /publish/1 형태로 발행해야 함을 설정
        // /publish로 시작하는 url패턴으로 메시지가 발행되면 @Controller 객체의 @MessageMapping메서드로 라우팅
        registry.setApplicationDestinationPrefixes("/publish");

        // /topic/1형태로 메시지를 수신(subscribe)해야 함을 설정
        registry.enableSimpleBroker("/topic");

    }

    // 웹소켓요청(connect, subscribe, disconnect)등의 요청시에는 http header등 http메시지를 넣어올 수 있고, interceptor를 통해 이를 가로채 토큰을 검증 할 수 있음.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }



}


