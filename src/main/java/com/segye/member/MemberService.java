package com.segye.member;

import com.segye.account.AccountRecordRepository;
import com.segye.auth.kakao.KakaoClient;
import com.segye.schedule.ScheduleRepository;
import com.segye.todo.TodoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final AccountRecordRepository accountRecordRepository;
    private final TodoRepository todoRepository;
    private final ScheduleRepository scheduleRepository;
    private final KakaoClient kakaoClient;

    public MemberService(MemberRepository memberRepository,
                         AccountRecordRepository accountRecordRepository,
                         TodoRepository todoRepository,
                         ScheduleRepository scheduleRepository,
                         KakaoClient kakaoClient) {
        this.memberRepository = memberRepository;
        this.accountRecordRepository = accountRecordRepository;
        this.todoRepository = todoRepository;
        this.scheduleRepository = scheduleRepository;
        this.kakaoClient = kakaoClient;
    }

    /** 회원과 회원의 데이터를 지우고, 지운 회원을 돌려준다. 카카오 연결 끊기는 커밋 뒤에 따로 한다. */
    @Transactional
    public Member deleteMemberData(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new SecurityException("회원이 없습니다."));
        accountRecordRepository.deleteByMember_Id(memberId);
        todoRepository.deleteByMember_Id(memberId);
        scheduleRepository.deleteByMember_Id(memberId);
        memberRepository.delete(member);
        return member;
    }

    /**
     * 탈퇴한 회원의 카카오 연결을 끊는다. 실패해도 탈퇴 자체는 이미 끝났으므로 로그만 남긴다.
     * 연결 끊기 실패로 탈퇴를 막으면 사용자가 데이터를 지울 방법이 없어진다.
     */
    public void unlinkKakaoQuietly(Member member) {
        if (member.getProvider() != AuthProvider.KAKAO) {
            return;
        }
        try {
            kakaoClient.unlink(Long.parseLong(member.getProviderId()));
        } catch (RuntimeException e) {
            log.warn("카카오 연결 끊기 실패(탈퇴는 완료됨): memberId={}, kakaoId={}",
                    member.getId(), member.getProviderId(), e);
        }
    }
}
