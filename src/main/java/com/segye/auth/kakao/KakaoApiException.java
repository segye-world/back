package com.segye.auth.kakao;

/**
 * 카카오 서버가 응답하지 않거나 5xx 를 준 경우. 사용자의 토큰 문제(401)와 구분해
 * 클라이언트가 "다시 로그인"이 아니라 "잠시 후 재시도"를 안내할 수 있게 한다.
 */
public class KakaoApiException extends RuntimeException {
    public KakaoApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public KakaoApiException(String message) {
        super(message);
    }
}
