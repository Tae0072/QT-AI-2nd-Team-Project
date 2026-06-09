package com.qtai.domain.member.client.sharing;

import com.qtai.domain.sharing.api.PurgeMemberSharingDataUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * sharing 도메인 {@link PurgeMemberSharingDataUseCase} 임시 Mock (MSA 통합 전).
 *
 * <p>sharing 도메인은 service-note로 분리되어 service-user 클래스패스에 실제 구현체가 없다.
 * Day3 통합에서 RestClient 어댑터로 교체하고 이 Mock은 삭제한다(CLAUDE.md §4).
 *
 * <p>안전 기본값: 삭제를 수행하지 않고 0을 반환한다.
 */
@Slf4j
@Component
public class PurgeMemberSharingDataUseCaseMock implements PurgeMemberSharingDataUseCase {

    @Override
    public int purgeByMemberId(Long memberId) {
        log.warn("[MOCK] sharing.PurgeMemberSharingDataUseCase — 통합 전 임시 구현(삭제 미수행): memberId={}", memberId);
        return 0;
    }
}
