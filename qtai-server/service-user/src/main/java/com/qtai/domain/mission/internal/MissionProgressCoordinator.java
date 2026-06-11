package com.qtai.domain.mission.internal;

import com.qtai.domain.mission.api.RecalculateMissionProgressUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 미션 진행률 재계산 코디네이터 (배치 진입점 구현).
 *
 * <p><b>트랜잭션을 직접 열지 않는다(비트랜잭션).</b> 회원별 작업은 {@link MissionProgressCalculator}
 * (별도 빈)의 {@code @Transactional} 메서드를 스프링 프록시 경유로 호출하므로 회원마다 독립 트랜잭션이
 * 시작된다. 따라서 한 회원의 실패가 rollback-only로 다른 회원/배치 전체를 롤백시키지 않는다.
 *
 * <p>ACTIVE 정의는 회원과 무관하게 동일하므로 배치 진입 시 1회만 로드해 각 회원 호출에 전달한다.
 *
 * <p><b>주의(2026-06-11, Mock→실어댑터 교체 파급):</b> {@link MissionProgressCalculator}가 쓰는
 * {@code GetMeditationCalendarUseCase} 구현이 사용자 JWT 전달 방식의 RestClient 어댑터
 * ({@code GetMeditationCalendarRestClientAdapter})로 교체되었다. 사용자 요청 컨텍스트가 없는
 * 배치/스케줄러에서 {@link #recalculateAllEnrolled()}를 호출하면 Authorization 없이 service-note를
 * 호출해 실패한다. 배치 도입 시 SYSTEM_BATCH 토큰 기반 호출(예: 수신측 시스템 토큰 허용 + 발급기 주입)을
 * 먼저 마련해야 한다. 현재(2026-06-11)는 이 메서드의 운영 호출자가 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionProgressCoordinator implements RecalculateMissionProgressUseCase {

    private final MissionProgressCalculator calculator;
    private final MissionDefinitionRepository definitionRepository;
    private final MemberMissionProgressRepository progressRepository;

    @Override
    public void recalculate(Long memberId) {
        calculator.recalculateForMember(memberId, activeDefinitions());
    }

    @Override
    public void recalculateAllEnrolled() {
        List<MissionDefinition> activeDefinitions = activeDefinitions(); // 1회 로드
        List<Long> memberIds = progressRepository.findDistinctMemberIds();
        log.info("미션 진행률 배치 시작: 대상 회원 {}명", memberIds.size());
        for (Long memberId : memberIds) {
            try {
                // 회원별 독립 트랜잭션(프록시 경유) — 한 회원 실패가 다음 회원으로 전파되지 않음
                calculator.recalculateForMember(memberId, activeDefinitions);
            } catch (Exception e) {
                log.warn("미션 진행률 재계산 실패: memberId={}", memberId, e);
            }
        }
    }

    private List<MissionDefinition> activeDefinitions() {
        return definitionRepository.findByStatus(MissionDefinitionStatus.ACTIVE);
    }
}
