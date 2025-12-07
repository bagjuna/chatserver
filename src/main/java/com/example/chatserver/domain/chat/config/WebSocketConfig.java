package com.example.chatserver.domain.chat.config;

//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.socket.config.annotation.EnableWebSocket;
//import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
//import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
//
//@Configuration
//@EnableWebSocke
//public class WebSocketConfig implements WebSocketConfigurer {
//
//    private final SimpleWebSocketHandler simpleWebSocketHandler;
//
//    public WebSocketConfig(SimpleWebSocketHandler simpleWebSocketHandler) {
//        this.simpleWebSocketHandler = simpleWebSocketHandler;
//    }
//
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        // connect url로 websocket 연결 요청 들어오면, 핸들러 클래스가 처리
//        registry.addHandler(simpleWebSocketHandler, "/connect")
//                // securityConfig에서의 cors 예외는 http요청에 대한 예외 따라서 websocket 프로토콜에 대한 요청에 대해서는 별도의 cors 설정 필요.
//                .setAllowedOrigins("http://localhost:3000");
//    }
//}
