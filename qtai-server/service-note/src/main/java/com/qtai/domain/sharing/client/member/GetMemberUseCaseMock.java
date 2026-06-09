package com.qtai.domain.sharing.client.member;

import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.member.api.dto.MemberResponse;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * member 도메인 {@link GetMemberUseCase}의 service-note 임시 구현(Mock).
 *
 * <p>MSA 분리 기준(CLAUDE.md §4): member는 service-user 소관이라 service-note에서는 api 계약 타입만
 * 가져와 client 어댑터로 임시 구현한다. 통합 시 이 Mock을 RestClient 호출 어댑터로 교체한다.
 *
 * <p>sharing은 닉네임(공개 필드)만 사용한다. 통합 전까지는 회원 id 기반의 합성 닉네임을 돌려준다.
 */
@Component("sharingMemberUseCaseMock")
public class GetMemberUseCaseMock implements GetMemberUseCase {

    @Override
    public MemberResponse getMember(Long memberId) {
        return new MemberResponse(
                memberId, nickname(memberId), null, null, "ACTIVE", "USER", null, null);
    }

    @Override
    public MemberPublicResponse getMemberPublic(Long memberId) {
        return new MemberPublicResponse(memberId, nickname(memberId), null);
    }

    @Override
    public List<MemberPublicResponse> getActivePublicProfiles(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return memberIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(id -> new MemberPublicResponse(id, nickname(id), null))
                .toList();
    }

    private static String nickname(Long memberId) {
        return "회원" + memberId;
    }
}
