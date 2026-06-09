package com.qtai.batch;

import com.qtai.domain.mission.api.RecalculateMissionProgressUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 미션 진행률 재계산 배치.
 *
 * <p>고정 제품 결정(CLAUDE.md §6): 수집 배치는 04:00 KST. 노트 활동 집계 결과를 반영해
 * 회원 미션 진행률을 재계산한다. 사용자 요청 경로가 아니라 SYSTEM_BATCH 주체로 동작한다.
 *
 * <p>대상은 진행률이 이미 존재하는(enroll된) 회원이다. 신규 enroll(최초 진행 레코드 생성)
 * 트리거는 후속 과제다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionProgressBatch {

    private final RecalculateMissionProgressUseCase recalculateMissionProgressUseCase;

    /** 매일 04:00 KST 미션 진행률 재계산. */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void recalculateDaily() {
        log.info("[batch] 미션 진행률 재계산 시작");
        recalculateMissionProgressUseCase.recalculateAllEnrolled();
        log.info("[batch] 미션 진행률 재계산 종료");
    }
}
