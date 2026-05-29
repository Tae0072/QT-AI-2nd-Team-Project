package com.qtai.domain.mission.internal;

import com.qtai.domain.mission.api.GetMemberMissionProgressUseCase;
import com.qtai.domain.mission.api.dto.MissionProgressResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미션 도메인 서비스 (읽기 모델).
 *
 * <p>API 명세서에 사용자용 /api/v1/missions 엔드포인트는 없다. 미션은 마이페이지 대시보드
 * (GET /api/v1/me/dashboard)의 {@code missionProgress}로만 노출되며, 진행률 수치는
 * 노트 활동 집계 배치가 갱신한다(ERD §2.24). 따라서 본 서비스는 진행률을 조회·매핑만 한다.
 *
 * <p>도메인 경계: member/note 등 다른 도메인 타입을 직접 import하지 않고 Long FK만 사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService implements GetMemberMissionProgressUseCase {

    private final MemberMissionProgressRepository progressRepository;
    private final MissionDefinitionRepository definitionRepository;

    @Override
    public List<MissionProgressResponse> getMissionProgress(Long memberId) {
        List<MemberMissionProgress> progresses =
                progressRepository.findByMemberIdOrderByPeriodStartDateDesc(memberId);
        if (progresses.isEmpty()) {
            return List.of();
        }

        List<Long> definitionIds = progresses.stream()
                .map(MemberMissionProgress::getMissionDefinitionId)
                .distinct()
                .toList();
        Map<Long, MissionDefinition> definitionMap = definitionRepository.findByIdIn(definitionIds).stream()
                .collect(Collectors.toMap(MissionDefinition::getId, Function.identity()));

        return progresses.stream()
                .map(p -> toResponse(p, definitionMap.get(p.getMissionDefinitionId())))
                .toList();
    }

    private MissionProgressResponse toResponse(MemberMissionProgress p, MissionDefinition def) {
        return new MissionProgressResponse(
                p.getMissionDefinitionId(),
                def != null ? def.getCode() : null,
                def != null ? def.getTitle() : null,
                def != null ? def.getMetricType().name() : null,
                def != null ? def.getPeriodType().name() : null,
                p.getCurrentCount(),
                p.getTargetCountSnapshot(),
                p.getProgressRate(),
                p.isCompleted(),
                p.getPeriodStartDate(),
                p.getPeriodEndDate(),
                p.getCompletedAt()
        );
    }
}
