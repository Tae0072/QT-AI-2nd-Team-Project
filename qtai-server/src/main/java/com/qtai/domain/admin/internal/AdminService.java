package com.qtai.domain.admin.internal;

/**
 * 관리자 도메인 진입점. 3개 UseCase 구현 + 트랜잭션 경계.
 *
 * admin은 거의 모든 도메인의 상태를 읽고 조작하므로, 타 도메인은 client/ 어댑터를
 * 통해 UseCase 인터페이스로만 접근한다 (도메인 간 엔티티 직접 참조 금지).
 * 모든 변경 액션은 AdminActionLog로 감사 기록을 남긴다.
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements GetStatsUseCase, LookupMemberUseCase, ModerateContentUseCase
public class AdminService {

    // TODO: final AdminActionLogRepository adminActionLogRepository;
    // TODO: final LookupMemberUseCase (실제 member 도메인 어댑터)
    // TODO: 콘텐츠 도메인 UseCase 어댑터들 (qt/note/sharing/report)

    // TODO: @Transactional hideContent(...) 구현
    //       콘텐츠 hidden=true 토글 → AdminActionLog 저장

    // TODO: @Transactional deleteContent(...) 구현
    //       콘텐츠 소프트 삭제 → AdminActionLog 저장

    // TODO: getStats(from, to) 구현 — 집계 쿼리 호출
}
