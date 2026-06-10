package com.qtai.domain.member.api.dto;

import com.qtai.domain.mission.api.dto.MissionProgressResponse;
import java.util.List;

/**
 * 마이페이지 대시보드 응답 DTO.
 *
 * API 명세서 §4.6.1 기준.
 * 위젯별 부분 실패 정책: 실패 위젯은 widgetErrors에 기록, 해당 데이터는 기본값.
 *
 * <p>missionProgress는 mission 도메인의 공개 계약({@link MissionProgressResponse})을 그대로 노출한다.
 */
public record DashboardResponse(
        ProfileSummary profile,
        StatsWidget stats,
        long unreadNotificationCount,
        PraiseSummary praiseSummary,
        List<MissionProgressResponse> missionProgress,
        List<String> widgetErrors
) {
    /** 방어적 복사 — 리스트 필드 외부 변경 방지. */
    public DashboardResponse {
        missionProgress = missionProgress == null ? List.of() : List.copyOf(missionProgress);
        widgetErrors = widgetErrors == null ? List.of() : List.copyOf(widgetErrors);
    }

    public record ProfileSummary(
            Long memberId,
            String nickname
    ) {}

    public record StatsWidget(
            WeekMonth week,
            WeekMonth month,
            int meditationStreakDays
    ) {
        public record WeekMonth(
                int savedNoteCount,
                int meditationDays
        ) {}
    }

    public record PraiseSummary(
            long savedSongCount
    ) {}
}
