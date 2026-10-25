package com.segye.common;

import com.segye.auth.kakao.KakaoApiException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.fail("VALIDATION_ERROR", msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("VALIDATION_ERROR", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurity(SecurityException e) {
        return ResponseEntity.status(401).body(ApiResponse.fail("UNAUTHORIZED", e.getMessage()));
    }

    // 사용자의 토큰 문제가 아니라 카카오 쪽 장애이므로 401 이 아니라 502 로 구분한다.
    @ExceptionHandler(KakaoApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleKakaoApi(KakaoApiException e) {
        log.warn("카카오 API 호출 실패: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(502)
                .body(ApiResponse.fail("KAKAO_UNAVAILABLE", "카카오 서버와 연결하지 못했습니다. 잠시 후 다시 시도해주세요."));
    }

    // 없는 경로는 아래 catch-all 에 걸려 500 이 되지 않도록 404 로 응답한다.
    // 없는 경로에 POST 하면 정적 리소스 핸들러가 받아 405 예외를 던지므로 그것도 함께 처리한다.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(404).body(ApiResponse.fail("NOT_FOUND", "요청한 경로를 찾을 수 없습니다."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405).body(ApiResponse.fail("METHOD_NOT_ALLOWED", "지원하지 않는 요청입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleEtc(Exception e) {
        return ResponseEntity.status(500).body(ApiResponse.fail("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }
}
