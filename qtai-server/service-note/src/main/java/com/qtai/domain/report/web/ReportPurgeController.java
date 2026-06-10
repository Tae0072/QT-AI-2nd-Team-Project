package com.qtai.domain.report.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.report.api.PurgeMemberReportDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 신고(접수) 데이터 정리 — 서비스 간 <b>내부 배치(SYSTEM_BATCH) 전용</b> 엔드포인트.
 *
 * <p>service-user 보존기간 만료 정리 배치가 호출한다. 회원이 접수한 신고 hard delete(타인 접수 신고는 유지)라
 * 사용자 경로로 노출하면 안 되므로 {@code @PreAuthorize("hasRole('SYSTEM_BATCH')")}로 시스템 배치 호출에만 허용한다.
 * 삭제는 {@code ReportPurgeService}의 자체 트랜잭션에서 커밋된다(분산 트랜잭션 없음, 멱등 재실행).
 */
@RestController
@RequiredArgsConstructor
public class ReportPurgeController {

    private final PurgeMemberReportDataUseCase purgeMemberReportDataUseCase;

    /** 해당 회원이 접수한 신고 데이터를 삭제하고 삭제 행 수를 돌려준다. */
    @PostMapping("/api/v1/reports/purge")
    @PreAuthorize("hasRole('SYSTEM_BATCH')")
    public ApiResponse<Integer> purge(@RequestParam Long memberId) {
        return ApiResponse.success(purgeMemberReportDataUseCase.purgeByMemberId(memberId));
    }
}
