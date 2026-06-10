package com.qtai.domain.member.client.praise;

import com.qtai.domain.praise.api.ListMemberPraiseSongUseCase;
import com.qtai.domain.praise.api.dto.MemberPraiseSongResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * praise 도메인 {@link ListMemberPraiseSongUseCase} 임시 Mock (MSA 통합 전).
 *
 * <p>마이페이지 대시보드의 찬양 요약 위젯이 사용한다. praise 도메인은 service-bible로
 * 분리되어 실제 구현체가 없다. Day3 통합에서 RestClient 어댑터로 교체하고 삭제한다(CLAUDE.md §4).
 *
 * <p>안전 기본값: 빈 목록/0건을 반환한다(대시보드 위젯은 0건으로 표시).
 */
@Slf4j
@Component
public class ListMemberPraiseSongUseCaseMock implements ListMemberPraiseSongUseCase {

    @Override
    public Page<MemberPraiseSongResponse> listMy(Long memberId, Pageable pageable) {
        log.warn("[MOCK] praise.ListMemberPraiseSongUseCase.listMy — 통합 전 임시 구현(빈 목록): memberId={}", memberId);
        return Page.empty(pageable);
    }

    @Override
    public long countMy(Long memberId) {
        log.warn("[MOCK] praise.ListMemberPraiseSongUseCase.countMy — 통합 전 임시 구현(0건): memberId={}", memberId);
        return 0L;
    }
}
