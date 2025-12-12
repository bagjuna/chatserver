package com.example.chatserver.global.common.BaseResponse;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import com.example.chatserver.global.common.error.ErrorCode;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class ErrorResponse<T> {
    private final int status;
    private final String error;
    private final String path;
    private final String code; // 추가적인 에러 코드
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    private final T data;

    /**
     * 기본 메시지 사용 (data 없음)
     */
    public static ResponseEntity<ErrorResponse<Void>> toResponseEntity(ErrorCode code, String path) {
        return toResponseEntity(code, path, null, null);
    }

    /**
     * 추가 메시지 사용 (data 없음)
     */
    public static ResponseEntity<ErrorResponse<Void>> toResponseEntity(ErrorCode code, String path, String message) {
        ErrorResponse body = ErrorResponse.builder()
                .status(code.getStatus().value())
                .error(message != null ? message : code.getMessage())
                .path(path)
                .build();
        return toResponseEntity(code, path, message, null);
    }

    /**
     * 기본 메시지 + data
     */
    public static <T> ResponseEntity<ErrorResponse<T>> toResponseEntity(
            ErrorCode code,
            String path,
            T data
    ) {
        return toResponseEntity(code, path, null, data);
    }

    /**
     * 추가 메시지 및 응답 데이터 사용
     * @param code 에러코드
     * @param path 요청한 위치
     * @param message 에러코드에 들어있는 것 외 추가 메시지
     * @param data 반환 데이터
     * @return ResponseEntity
     * @param <T> 반환 데이터 타입
     */
    public static <T> ResponseEntity<ErrorResponse<T>> toResponseEntity(
            ErrorCode code,
            String path,
            String message,
            T data
    ) {
        ErrorResponse<T> body = ErrorResponse.<T>builder()
                .status(code.getStatus().value())
                .error(message != null ? message : code.getMessage())
                .path(path)
                .data(data)
                .build();
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    public static <T> ErrorResponse<T> of(ErrorCode code, String path, T data) {
        return ErrorResponse.<T>builder()
                .status(code.getStatus().value())
                .error(code.getMessage())
                .path(path)
                .build();
    }
}
