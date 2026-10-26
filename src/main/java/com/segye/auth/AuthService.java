package com.segye.auth;

import com.segye.auth.kakao.KakaoClient;
import com.segye.auth.kakao.KakaoProperties;
import com.segye.member.AuthProvider;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private static final String KAKAO_AUTH_FAILED = "카카오 인증에 실패했습니다. 다시 로그인해주세요.";

    private final MemberRepository memberRepository;
    private final JwtTokenProvider tokenProvider;
    private final KakaoClient kakaoClient;
    private final KakaoProperties kakaoProperties;

    public AuthService(MemberRepository memberRepository,
                       JwtTokenProvider tokenProvider,
                       KakaoClient kakaoClient,
                       KakaoProperties kakaoProperties) {
        this.memberRepository = memberRepository;
        this.tokenProvider = tokenProvider;
        this.kakaoClient = kakaoClient;
        this.kakaoProperties = kakaoProperties;
    }

    public record LoginResult(String accessToken, boolean isNewMember) {
    }

    // 카카오 API 호출이 DB 트랜잭션을 붙잡지 않도록 메서드 전체를 트랜잭션으로 묶지 않는다.
    // 조회·저장은 리포지토리 메서드 단위 트랜잭션으로 충분하다.
    public LoginResult loginWithKakao(String kakaoAccessToken) {
        Long expectedAppId = kakaoProperties.getAppId();
        if (expectedAppId == null) {
            log.error("app.kakao.app-id(KAKAO_APP_ID)가 설정되지 않아 카카오 로그인을 거절합니다.");
            throw new IllegalStateException("카카오 로그인이 설정되지 않았습니다.");
        }

        // /v2/user/me 만으로는 다른 앱에서 발급된 토큰도 통과한다. 다른 앱이 자기 사용자에게서 받은
        // 토큰으로 우리 서비스에 그 사용자로 로그인하는 것을 막기 위해 발급 앱 ID 를 먼저 확인한다.
        KakaoClient.TokenInfo tokenInfo = kakaoClient.getTokenInfo(kakaoAccessToken);
        if (!expectedAppId.equals(tokenInfo.appId())) {
            log.warn("다른 앱에서 발급된 카카오 토큰으로 로그인 시도: appId={}", tokenInfo.appId());
            throw new SecurityException(KAKAO_AUTH_FAILED);
        }

        KakaoClient.KakaoUser kakaoUser = kakaoClient.getUser(kakaoAccessToken);
        if (kakaoUser.id() == null || !kakaoUser.id().equals(tokenInfo.id())) {
            log.warn("카카오 토큰 정보와 사용자 정보의 회원번호가 다릅니다: tokenInfo={}, user={}",
                    tokenInfo.id(), kakaoUser.id());
            throw new SecurityException(KAKAO_AUTH_FAILED);
        }

        String providerId = String.valueOf(kakaoUser.id());
        Optional<Member> existing = memberRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId);
        if (existing.isPresent()) {
            Member member = existing.get();
            // 카카오에서 닉네임을 바꿨으면 따라간다.
            if (kakaoUser.nickname() != null && !Objects.equals(member.getNickname(), kakaoUser.nickname())) {
                member.changeNickname(kakaoUser.nickname());
                memberRepository.save(member);
            }
            return new LoginResult(tokenProvider.createAccessToken(member.getId()), false);
        }

        try {
            Member created = memberRepository.saveAndFlush(
                    new Member(AuthProvider.KAKAO, providerId, kakaoUser.nickname()));
            return new LoginResult(tokenProvider.createAccessToken(created.getId()), true);
        } catch (DataIntegrityViolationException e) {
            // 첫 로그인 요청이 동시에 두 번 들어와 다른 요청이 먼저 가입시킨 경우다.
            Member member = memberRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
                    .orElseThrow(() -> e);
            return new LoginResult(tokenProvider.createAccessToken(member.getId()), false);
        }
    }
}
