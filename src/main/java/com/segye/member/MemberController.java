package com.segye.member;

import com.segye.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("인증이 필요합니다.");
        }
        Long memberId = (Long) auth.getPrincipal();
        // 데이터 삭제가 커밋된 뒤에 연결을 끊는다. 순서가 반대면 삭제가 롤백돼도 연결은 이미 끊겨 있다.
        Member deleted = memberService.deleteMemberData(memberId);
        memberService.unlinkKakaoQuietly(deleted);
        return ApiResponse.ok();
    }
}
