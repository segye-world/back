package com.segye.auth.review;

import com.segye.auth.AuthSessionService;
import com.segye.auth.dto.AuthDtos.TokenResponse;
import com.segye.member.AuthProvider;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.member.MemberService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 스토어 심사용 로그인. 심사자는 카카오 계정 없이 Play Console 에 적어 둔 아이디·비밀번호로 로그인하고,
 * 항상 같은 심사용 회원 (REVIEW, "reviewer") 으로 들어간다. 탈퇴를 테스트해 지워지면 다음 로그인 때 다시 만든다.
 *
 * 운영에서도 켜 두는 경로이므로
 * - 아이디·비밀번호 비교는 시간이 일정하게 하고, 아이디가 틀려도 bcrypt 검사는 항상 한다.
 * - 계정이 하나뿐이라 실패 횟수는 전체 요청 기준으로 센다. 연속 실패가 maxFailures 에 닿으면 잠근다.
 *   서버가 여러 대면 대마다 따로 센다(지금은 한 대).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.auth.review-login.enabled", havingValue = "true")
public class ReviewLoginService {

    static final String REVIEWER_PROVIDER_ID = "reviewer";
    static final String REVIEWER_NICKNAME = "심사자";
    private static final String LOGIN_FAILED = "아이디 또는 비밀번호가 올바르지 않습니다.";
    private static final Pattern BCRYPT = Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final ReviewLoginProperties props;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final AuthSessionService sessionService;
    private final TransactionTemplate transactions;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    Clock clock = Clock.systemUTC();
    private int consecutiveFailures = 0;
    private Instant lockedUntil = Instant.MIN;

    public ReviewLoginService(ReviewLoginProperties props,
                              MemberRepository memberRepository,
                              MemberService memberService,
                              AuthSessionService sessionService,
                              TransactionTemplate transactions) {
        this.props = props;
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.sessionService = sessionService;
        this.transactions = transactions;
    }

    // 켜 놓고 값을 빠뜨리면 심사 도중에야 알게 되므로 부팅 시 실패시킨다.
    @PostConstruct
    void validateConfig() {
        if (props.getLoginId() == null || props.getLoginId().isBlank()) {
            throw new IllegalStateException("REVIEW_LOGIN_ENABLED=true 인데 REVIEW_LOGIN_ID 가 비어 있습니다.");
        }
        if (props.getPasswordHash() == null || !BCRYPT.matcher(props.getPasswordHash()).matches()) {
            throw new IllegalStateException("REVIEW_LOGIN_PASSWORD_HASH 가 bcrypt 해시가 아닙니다. 평문을 넣지 마세요.");
        }
        log.warn("심사용 로그인(POST /api/v1/auth/review-login)이 켜져 있습니다.");
    }

    public TokenResponse login(String loginId, String password) {
        checkNotLocked();

        boolean idMatches = MessageDigest.isEqual(
                props.getLoginId().getBytes(StandardCharsets.UTF_8), loginId.getBytes(StandardCharsets.UTF_8));
        boolean passwordMatches = encoder.matches(password, props.getPasswordHash());
        if (!(idMatches && passwordMatches)) {
            recordFailure();
            throw new SecurityException(LOGIN_FAILED);
        }
        recordSuccess();

        Optional<Member> existing = findReviewer();
        if (existing.isPresent()) {
            return issueSession(existing.get(), false);
        }
        try {
            return transactions.execute(status -> issueSession(
                    memberService.registerMember(AuthProvider.REVIEW, REVIEWER_PROVIDER_ID, REVIEWER_NICKNAME), true));
        } catch (DataIntegrityViolationException e) {
            // 첫 로그인이 동시에 두 번 들어와 다른 요청이 먼저 만든 경우다.
            return issueSession(findReviewer().orElseThrow(() -> e), false);
        }
    }

    private Optional<Member> findReviewer() {
        return memberRepository.findByProviderAndProviderId(AuthProvider.REVIEW, REVIEWER_PROVIDER_ID);
    }

    private TokenResponse issueSession(Member member, boolean newMember) {
        var tokens = sessionService.create(member.getId());
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), newMember);
    }

    private synchronized void checkNotLocked() {
        if (clock.instant().isBefore(lockedUntil)) {
            throw new ReviewLoginLockedException();
        }
    }

    private synchronized void recordFailure() {
        consecutiveFailures++;
        log.warn("심사용 로그인 실패: 연속 {}회", consecutiveFailures);
        if (consecutiveFailures >= props.getMaxFailures()) {
            lockedUntil = clock.instant().plus(props.getLockDuration());
            consecutiveFailures = 0;
            log.warn("심사용 로그인을 {} 동안 잠급니다.", props.getLockDuration());
        }
    }

    private synchronized void recordSuccess() {
        consecutiveFailures = 0;
        log.info("심사용 로그인 성공");
    }

    // 테스트들이 같은 스프링 컨텍스트(같은 빈)를 쓰므로 실패 횟수를 초기화할 수 있게 둔다.
    synchronized void resetFailures() {
        consecutiveFailures = 0;
        lockedUntil = Instant.MIN;
    }
}
