package com.qtai.domain.mission.api;

import com.qtai.domain.mission.api.dto.MissionProgressResponse;
import java.util.List;

/**
 * 회원 미션 진행률 조회 UseCase 포트.
 *
 * <p>마이페이지 대시보드(me 도메인)가 호출해 {@code missionProgress} 위젯을 채운다.
 * 미션은 사용자 직접 시작/완료 API가 없으며(API 명세서에 /api/v1/missions 미존재),
 * 진행률은 노트 활동 집계 배치가 갱신한 결과를 조회만 한다.
 */
public interface GetMemberMissionProgressUseCase {

    /**
     * 회원의 미션 진행률 목록을 집계 시작일 내림차순으로 조회한다.
     *
     * @param memberId 조회 대상 회원 ID
     * @return 진행률 목록 (없으면 빈 리스트)
     */
    List<MissionProgressResponse> getMissionProgress(Long memberId);
}
