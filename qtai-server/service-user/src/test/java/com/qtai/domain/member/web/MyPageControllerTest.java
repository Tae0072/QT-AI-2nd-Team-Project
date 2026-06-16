package com.qtai.domain.member.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.dto.DashboardResponse;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.mission.api.GetMemberMissionProgressUseCase;
import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse.Summary;
import com.qtai.domain.notification.api.ListNotificationUseCase;
import com.qtai.domain.praise.api.ListMemberPraiseSongUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
// (strict stubs 기본 — 통계 실패 테스트는 stubOthersSuccess만 사용해 미사용 스텁 경고를 피한다)

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * {@link MyPageController} 대시보드 통합 동작 테스트 — 특히 <b>위젯별 부분 실패 격리</b>를 검증한다.
 *
 * <p>대시보드 정책(API 명세서 §4.6.1): 한 위젯의 조회 실패가 전체 응답을 실패시키지 않고,
 * 실패 위젯은 기본값 + {@code widgetErrors}에 이름이 기록되어야 한다.
 */
@ExtendWith(MockitoExtension.class)
class MyPageControllerTest {

    /** 2026-06-10(수) KST — 주(6/8 월~)가 6월 안에 있어 달력 1회 조회 경로. */
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-06-10T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private GetMemberUseCase getMemberUseCase;
    @Mock
    private ListNotificationUseCase listNotificationUseCase;
    @Mock
    private ListMemberPraiseSongUseCase listMemberPraiseSongUseCase;
    @Mock
    private GetMemberMissionProgressUseCase getMemberMissionProgressUseCase;
    @Mock
    private GetMeditationCalendarUseCase getMeditationCalendarUseCase;

    @InjectMocks
    private MyPageController controller;

    private static final long MEMBER_ID = 7L;

    private void stubAllSuccess() {
        stubOthersSuccess();
        stubCalendarSuccess();
    }

    /** 달력(통계) 제외 위젯 성공 스텁 — 통계 실패 테스트에서 미사용 스텁 경고를 피하기 위해 분리. */
    private void stubOthersSuccess() {
        when(getMemberUseCase.getMember(MEMBER_ID)).thenReturn(
                new MemberResponse(MEMBER_ID, "새벽이슬", "e@test.dev", null, null, null, null, null));
        when(listNotificationUseCase.countUnread(MEMBER_ID)).thenReturn(2L);
        when(listMemberPraiseSongUseCase.countMy(MEMBER_ID)).thenReturn(3L);
        when(getMemberMissionProgressUseCase.getMissionProgress(MEMBER_ID)).thenReturn(List.of());
    }

    private void stubCalendarSuccess() {
        when(getMeditationCalendarUseCase.getCalendar(eq(MEMBER_ID), any(YearMonth.class)))
                .thenReturn(new MeditationCalendarResponse(
                        "2026-06", List.of(), new Summary(2, 3, 4)));
    }

    @Test
    @DisplayName("인증 주체가 없으면(UNAUTHORIZED) 위젯 조회 없이 즉시 거절한다")
    void dashboard_비인증_거절() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.dashboard(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("전체 성공 — 통계가 달력 summary로 채워지고 widgetErrors는 비어 있다")
    void dashboard_전체성공() {
        stubAllSuccess();

        DashboardResponse body = controller.dashboard(MEMBER_ID).getBody().data();

        assertThat(body.widgetErrors()).isEmpty();
        assertThat(body.profile().nickname()).isEqualTo("새벽이슬");
        assertThat(body.unreadNotificationCount()).isEqualTo(2);
        assertThat(body.praiseSummary().savedSongCount()).isEqualTo(3);
        assertThat(body.stats().month().meditationDays()).isEqualTo(2);  // summary.savedDays
        assertThat(body.stats().month().savedNoteCount()).isEqualTo(3);
        assertThat(body.stats().meditationStreakDays()).isEqualTo(4);
        // 6/10(수)의 주는 6월 안 — 이번 달 1회만 조회
        verify(getMeditationCalendarUseCase, times(1)).getCalendar(eq(MEMBER_ID), any(YearMonth.class));
    }

    @Test
    @DisplayName("부분 실패 — 통계(달력 호출) 실패 시 stats만 0으로 격리되고 나머지 위젯은 정상이다")
    void dashboard_통계_실패_격리() {
        stubOthersSuccess();
        when(getMeditationCalendarUseCase.getCalendar(eq(MEMBER_ID), any(YearMonth.class)))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_FAILURE));

        DashboardResponse body = controller.dashboard(MEMBER_ID).getBody().data();

        assertThat(body.widgetErrors()).containsExactly("stats");
        assertThat(body.stats().week().meditationDays()).isZero();
        assertThat(body.stats().month().meditationDays()).isZero();
        assertThat(body.stats().meditationStreakDays()).isZero();
        // 다른 위젯은 영향 없음
        assertThat(body.profile().nickname()).isEqualTo("새벽이슬");
        assertThat(body.unreadNotificationCount()).isEqualTo(2);
        assertThat(body.praiseSummary().savedSongCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("부분 실패 — 프로필 실패 시 profile만 기본값(빈 닉네임)으로 격리된다")
    void dashboard_프로필_실패_격리() {
        stubAllSuccess();
        when(getMemberUseCase.getMember(MEMBER_ID)).thenThrow(new RuntimeException("DB down"));

        DashboardResponse body = controller.dashboard(MEMBER_ID).getBody().data();

        assertThat(body.widgetErrors()).containsExactly("profile");
        assertThat(body.profile().memberId()).isEqualTo(MEMBER_ID);
        assertThat(body.profile().nickname()).isEmpty();
        assertThat(body.stats().meditationStreakDays()).isEqualTo(4); // 통계는 정상
    }

    @Test
    @DisplayName("다중 부분 실패 — 알림·찬양·미션이 모두 실패해도 200 응답에 widgetErrors 3건으로 보고된다")
    void dashboard_다중_실패_격리() {
        stubAllSuccess();
        when(listNotificationUseCase.countUnread(MEMBER_ID)).thenThrow(new RuntimeException("redis down"));
        when(listMemberPraiseSongUseCase.countMy(MEMBER_ID)).thenThrow(new RuntimeException("praise down"));
        when(getMemberMissionProgressUseCase.getMissionProgress(MEMBER_ID))
                .thenThrow(new RuntimeException("mission down"));

        DashboardResponse body = controller.dashboard(MEMBER_ID).getBody().data();

        assertThat(body.widgetErrors())
                .containsExactlyInAnyOrder("unreadNotificationCount", "praiseSummary", "missionProgress");
        assertThat(body.unreadNotificationCount()).isZero();
        assertThat(body.praiseSummary().savedSongCount()).isZero();
        assertThat(body.missionProgress()).isEmpty();
        assertThat(body.profile().nickname()).isEqualTo("새벽이슬"); // 성공 위젯 유지
    }

    @Test
    @DisplayName("월 경계 주 — 주 시작(월요일)이 이전 달이면 달력을 2회(이번 달+이전 달) 조회한다")
    void dashboard_월경계_달력_2회_조회() {
        // 2026-07-01(수) KST: 주 시작 6/29(월) — 이전 달 추가 조회 경로
        Clock crossing = Clock.fixed(Instant.parse("2026-07-01T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        MyPageController crossingController = new MyPageController(
                getMemberUseCase, listNotificationUseCase, listMemberPraiseSongUseCase,
                getMemberMissionProgressUseCase, getMeditationCalendarUseCase, crossing);
        stubAllSuccess();

        DashboardResponse body = crossingController.dashboard(MEMBER_ID).getBody().data();

        assertThat(body.widgetErrors()).isEmpty();
        verify(getMeditationCalendarUseCase).getCalendar(MEMBER_ID, YearMonth.of(2026, 7));
        verify(getMeditationCalendarUseCase).getCalendar(MEMBER_ID, YearMonth.of(2026, 6));
    }
}
