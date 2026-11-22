package com.segye.auth.review;

/** 심사용 로그인이 연속 실패로 잠시 잠긴 상태. 429 로 응답한다. */
public class ReviewLoginLockedException extends RuntimeException {

    public ReviewLoginLockedException() {
        super("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
    }
}
