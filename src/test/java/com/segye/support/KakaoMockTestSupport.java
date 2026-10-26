package com.segye.support;

import com.segye.auth.kakao.KakaoClient;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * 카카오 API 를 목으로 바꾼 통합 테스트의 베이스. @MockBean 선언을 한곳에 두어야
 * 하위 테스트들이 같은 스프링 컨텍스트를 재사용한다.
 */
public abstract class KakaoMockTestSupport extends PostgresTestSupport {

    // application-test.yml 의 app.kakao.app-id 와 같은 값
    protected static final long OUR_APP_ID = 1000001L;

    @MockBean
    protected KakaoClient kakaoClient;
}
