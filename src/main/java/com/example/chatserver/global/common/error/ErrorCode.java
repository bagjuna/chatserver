package com.example.chatserver.global.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // BAD REQUEST
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다. 필요한 요청 데이터가 없거나, 지원되지 않는 형식입니다."),
    PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth Provider 입니다."),
    USERNAME_TAKEN(HttpStatus.BAD_REQUEST, "이미 사용 중인 username 입니다."),
    INVALID_USERNAME(HttpStatus.BAD_REQUEST, "유효하지 않은 username 입니다."),
    INVALID_LOGIN_SESSION(HttpStatus.BAD_REQUEST, "유효하지 않은 로그인 세션입니다. 다시 시도해주세요."),

    // NOT FOUND
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다. URL을 확인하세요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 ID의 유저가 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 ID의 게시물이 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 ID의 댓글이 없습니다."),
    ALREADY_EXIST_EMAIL(HttpStatus.NOT_FOUND, "이미 존재하는 이메일입니다."),

    // METHOD NOT ALLOWED
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

    // UNAUTHORIZED
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요한 요청입니다."),
    JWT_AUTHENTICATION_FAIL(HttpStatus.UNAUTHORIZED, "JWT 인증에 실패하였습니다."),
    REQUIRES_SIGNUP(HttpStatus.UNAUTHORIZED, "회원가입이 필요합니다. 쿠키에 sessionId가 발급되었습니다. '/auth/signup' 으로 가입 요청을 진행해주세요."),
    WRONG_OAUTH2_PROVIDER(HttpStatus.UNAUTHORIZED, "잘못된 OAuth 2.0 Provider 입니다. 응답 데이터 내 provider 로 이미 가입되어 있습니다."),
    STOMP_CONNECT_JWT_AUTHENTICATION_FAIL(HttpStatus.UNAUTHORIZED, "STOMP 연결을 위한 JWT 인증에 실패하였습니다."),

    // FORBIDDEN
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    USER_NOT_ALLOWED_UPDATE_POST(HttpStatus.FORBIDDEN, "해당 게시물을 수정할 권한이 없습니다."),
    USER_NOT_ALLOWED_DELETE_POST(HttpStatus.FORBIDDEN, "해당 게시물을 삭제할 권한이 없습니다."),
    USER_NOT_ALLOWED_UPDATE_COMMENT(HttpStatus.FORBIDDEN, "해당 댓글을 수정할 권한이 없습니다."),
    USER_NOT_ALLOWED_DELETE_COMMENT(HttpStatus.FORBIDDEN, "해당 댓글을 삭제할 권한이 없습니다."),

    // INTERNAL SERVER ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");

    private final HttpStatus status;
    private final String message;
}
