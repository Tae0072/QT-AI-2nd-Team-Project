package com.qtai.domain.report.internal;

/**
 * 신고 도메인 진입점. 2개 UseCase 구현 + 트랜잭션 경계.
 *
 * 타 도메인 접근은 client/ 어댑터로만:
 *   - member.GetMemberUseCase             — 신고자 검증
 *   - sharing.GetSharedSnapshotUseCase    — 신고 대상 스냅샷 존재/가시성 검증
 *
 * 후처리: 신고가 임계값 초과 시 admin 알림 또는 자동 hidden — 별도 정책 결정 필요.
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements CreateReportUseCase, GetReportUseCase
public class ReportService {

    // TODO: final ReportRepository reportRepository;
    // TODO: final GetMemberUseCase getMemberUseCase;
    // TODO: final GetSharedSnapshotUseCase getSharedSnapshotUseCase;

    // TODO: @Transactional createReport — 중복 검증 + 대상 존재 검증 후 INSERT (status=PENDING)
    // TODO: getReport(viewerId, reportId) — 본인 또는 ADMIN만 조회 가능
    // TODO: listMy(memberId, pageable) — 본인 신고 이력
}
