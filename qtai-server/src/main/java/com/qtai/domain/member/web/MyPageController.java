package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.DashboardResponse;
import com.qtai.domain.member.api.dto.DashboardResponse.PraiseSummary;
import com.qtai.domain.member.api.dto.DashboardResponse.ProfileSummary;
import com.qtai.domain.member.api.dto.DashboardResponse.StatsWidget;
import com.qtai.domain.member.api.dto.DashboardResponse.StatsWidget.WeekMonth;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import com.qtai.domain.notification.api.ListNotificationUseCase;
import com.qtai.domain.praise.api.ListMemberPraiseSongUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 마이페이지 REST 엔드포인트.
 *
 * <p>API 명세서 §4.6.1~§4.6.2 기준.
 * <p>위젯별 부분 실패: 한 위젯 조회 실패가 전체 응답을 실패시키지 않는다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MyPageController {

    private final GetMemberUseCase getMemberUseCase;
    private final ListNotificationUseCase listNotificationUseCase;
    private final ListMemberPraiseSongUseCase listMemberPraiseSongUseCase;
    private final GetMeditationCalendarUseCase getMeditationCalendarUseCase;

    /**
     * GET /api/v1/me/dashboard — 대시보드.
     */
    @GetMapping("/api/v1/me/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard(
            @AuthenticationPrincipal Long memberId) {

        List<String> widgetErrors = new ArrayList<>();

        // ── 프로필 ──
        ProfileSummary profile = loadProfile(memberId, widgetErrors);

        // ── 통계 (notes 도메인 미구현 — 기본값) ──
        StatsWidget stats = loadStats(memberId, widgetErrors);

        // ── 미읽음 알림 수 ──
        long unreadCount = loadUnreadNotificationCount(memberId, widgetErrors);

        // ── 찬양 요약 ──
        PraiseSummary praiseSummary = loadPraiseSummary(memberId, widgetErrors);

        DashboardResponse response = new DashboardResponse(
                profile, stats, unreadCount, praiseSummary, widgetErrors);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/me/meditation-calendar?month=2026-05
     * 묵상 달력.
     */
    @GetMapping("/api/v1/me/meditation-calendar")
    public ResponseEntity<ApiResponse<MeditationCalendarResponse>> meditationCalendar(
            @AuthenticationPrincipal Long memberId,
            @RequestParam String month) {
        Long authenticatedMemberId = requireMemberId(memberId);
        YearMonth targetMonth = parseMonth(month);
        return ResponseEntity.ok(ApiResponse.success(
                getMeditationCalendarUseCase.get(authenticatedMemberId, targetMonth)
        ));
    }

    // ── private widget loaders (부분 실패 허용) ──

    private ProfileSummary loadProfile(Long memberId, List<String> errors) {
        try {
            MemberResponse member = getMemberUseCase.getMember(memberId);
            return new ProfileSummary(member.id(), member.nickname());
        } catch (Exception e) {
            log.warn("대시보드 프로필 위젯 실패: memberId={}", memberId, e);
            errors.add("profile");
            return new ProfileSummary(memberId, "");
        }
    }

    // TODO: notes 도메인 연동 시 memberId 파라미터 사용 + errors 기록 로직 추가
    @SuppressWarnings("unused")
    private StatsWidget loadStats(Long memberId, List<String> errors) {
        // notes 도메인 미구현 — 기본값 반환
        return new StatsWidget(
                new WeekMonth(0, 0),
                new WeekMonth(0, 0),
                0
        );
    }

    private long loadUnreadNotificationCount(Long memberId, List<String> errors) {
        try {
            return listNotificationUseCase.countUnread(memberId);
        } catch (Exception e) {
            log.warn("대시보드 알림 위젯 실패: memberId={}", memberId, e);
            errors.add("unreadNotificationCount");
            return 0;
        }
    }

    private PraiseSummary loadPraiseSummary(Long memberId, List<String> errors) {
        try {
            long count = listMemberPraiseSongUseCase.countMy(memberId);
            return new PraiseSummary(count);
        } catch (Exception e) {
            log.warn("대시보드 찬양 위젯 실패: memberId={}", memberId, e);
            errors.add("praiseSummary");
            return new PraiseSummary(0);
        }
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
