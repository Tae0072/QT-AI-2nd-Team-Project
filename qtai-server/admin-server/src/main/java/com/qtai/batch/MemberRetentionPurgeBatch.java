package com.qtai.batch;

import com.qtai.domain.member.api.PurgeExpiredWithdrawnMembersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 탈퇴 회원 보존기간(2년) 만료 정리 배치.
 *
 * <p>정책(2026-06-05 Lead 결정): 탈퇴 시 "개인정보와 작성 기록 2년 보관 후 자동 삭제"를
 * 고지하며, 이 배치가 매일 03:00 KST에 보존기간이 지난 탈퇴 회원을 hard delete한다.
 * (사용자 노출 갱신 04:00 KST·해설 시딩 00:05 KST 배치와 시간대를 분리해 부하를 피한다)
 *
 * <p>사용자 요청 경로가 아니라 SYSTEM_BATCH 주체로 동작한다. 회원 단위 트랜잭션으로
 * 처리하므로 일부 실패는 로그로 남고 다음 회원 처리를 막지 않는다(재처리 가능 상태 유지).
 *
 * <p><b>활성화 게이트</b>({@code qtai.retention.purge.enabled}): hard delete는 비가역이라 기본 off로 두고
 * 운영(prod)에서만 켠다(개발/로컬/테스트 데이터가 실수로 삭제되지 않도록). 관리자 연결 회원 제외는
 * {@code MemberRetentionPurgeService}가 admin 도메인 포트({@code VerifyAdminRoleUseCase})로 판정한다 —
 * 이 판정이 신뢰 가능해진 뒤(admin 검증 통합 완료) 활성화한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "qtai.retention.purge", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class MemberRetentionPurgeBatch {

    private final PurgeExpiredWithdrawnMembersUseCase purgeExpiredWithdrawnMembersUseCase;

    /** 매일 03:00 KST 보존기간 만료 탈퇴 회원 정리. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void purgeDaily() {
        log.info("[batch] 탈퇴 회원 보존기간 만료 정리 시작");
        int purged = purgeExpiredWithdrawnMembersUseCase.purgeExpired();
        log.info("[batch] 탈퇴 회원 보존기간 만료 정리 종료: purgedCount={}", purged);
    }
}
