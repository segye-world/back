package com.segye.auth;

import com.segye.auth.dto.AuthDtos.TokenResponse;
import com.segye.auth.kakao.KakaoClient;
import com.segye.auth.kakao.KakaoProperties;
import com.segye.member.AuthProvider;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.member.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private static final String KAKAO_AUTH_FAILED = "카카오 인증에 실패했습니다. 다시 로그인해주세요.";

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final AuthSessionService sessionService;
    private final KakaoClient kakaoClient;
    private final KakaoProperties kakaoProperties;
    private final TransactionTemplate transactions;

    public AuthService(MemberRepository memberRepository,
                       MemberService memberService,
                       AuthSessionService sessionService,
                       KakaoClient kakaoClient,
                       KakaoProperties kakaoProperties,
                       TransactionTemplate transactions) {
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.sessionService = sessionService;
        this.kakaoClient = kakaoClient;
        this.kakaoProperties = kakaoProperties;
        this.transactions = transactions;
    }

    // 카카오 API 호출이 DB 트랜잭션을 붙잡지 않도록 메서드 전체를 트랜잭션으로 묶지 않는다.
    public TokenResponse loginWithKakao(String kakaoAccessToken) {
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
            return issueSession(member, false);
        }

        try {
            return transactions.execute(status ->
                    issueSession(memberService.registerMember(AuthProvider.KAKAO, providerId, kakaoUser.nickname()), true));
        } catch (DataIntegrityViolationException e) {
            // 첫 로그인 요청이 동시에 두 번 들어와 다른 요청이 먼저 가입시킨 경우다.
            Member member = memberRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
                    .orElseThrow(() -> e);
            return issueSession(member, false);
        }
    }

    private TokenResponse issueSession(Member member, boolean newMember) {
        var tokens = sessionService.create(member.getId());
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), newMember);
    }
}
