import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:table_calendar/table_calendar.dart';

import '../../../core/dev/web_dev_access.dart';
import '../../../core/theme/app_theme.dart';
import '../models/note_models.dart';
import '../providers/note_providers.dart';
import 'note_day_checklist_sheet.dart';

/// 카테고리별 색점 색상 — Calm Paper 톤(저채도 어스톤).
///
/// 예약색 accentDot(#E0492F)은 활성 탭 도트 전용이므로 카테고리 점에는 쓰지 않는다.
/// 5종 모두 저채도 어스톤으로 구분하며, QT만 따뜻한 클레이로 살짝 도드라지게 한다.
/// Map의 키 순서가 색점 표시 순서다.
const Map<String, Color> kNoteCategoryDotColors = {
  'MEDITATION': Color(0xFFB5654D), // QT — 더스티 클레이(탭 accentDot과 분리)
  'SERMON': Color(0xFF7E8A93), // 설교 — 더스티 슬레이트
  'PRAYER': Color(0xFF84937E), // 기도 — 더스티 세이지
  'REPENTANCE': Color(0xFF94858F), // 회개 — 더스티 모브
  'GRATITUDE': Color(0xFFB0A077), // 감사 — 더스티 샌드
};

/// 한 날짜에 표시할 색점 최대 개수(초과 시 잘라낸다 — 체크리스트 바텀시트에서 전체 확인).
const int _kMaxDots = 3;

/// 묵상 달력 (N-01의 달력 모드 상단).
///
/// - GET /me/meditation-calendar?month=yyyy-MM 을 월별로 조회(meditationCalendarProvider)
/// - 저장된 날에 카테고리별 색점(최대 3개) 표시. 날짜 탭 시 그날 기록 체크리스트 바텀시트.
/// - 이전/다음 달로 넘기면 그 달을 다시 조회.
/// - 월/일주일 보기 토글 제공(⑦). 요일행 높이 확보로 잘림 방지(⑥).
/// - Calm Paper: 날짜/오늘/선택 스타일은 무채색, 저장일 점만 카테고리 색(④).
/// - 달력은 항상 그리고(고정 높이) 점 데이터만 응답이 오면 채운다 → 아래에 노트 목록을
///   Expanded로 붙일 수 있다. 요약(저장일·연속일)은 마이페이지에 있으므로 여기선 제거.
class MeditationCalendarView extends ConsumerStatefulWidget {
  const MeditationCalendarView({super.key});

  @override
  ConsumerState<MeditationCalendarView> createState() =>
      _MeditationCalendarViewState();
}

class _MeditationCalendarViewState
    extends ConsumerState<MeditationCalendarView> {
  DateTime _focusedDay = DateTime.now(); // 지금 보고 있는 달
  DateTime? _selectedDay;
  CalendarFormat _calendarFormat = CalendarFormat.month; // ⑦ month/week 토글 상태

  // "yyyy-MM" — provider/API에 넘길 월 키.
  String get _monthKey =>
      '${_focusedDay.year}-${_focusedDay.month.toString().padLeft(2, '0')}';

  // ✏️ DateTime은 시·분이 붙어 비교가 어긋나므로, 비교용 키는 날짜만 남긴다.
  DateTime _dateKey(DateTime d) => DateTime.utc(d.year, d.month, d.day);

  @override
  Widget build(BuildContext context) {
    final calendarAsync = ref.watch(meditationCalendarProvider(_monthKey));
    final c = context.appColors;
    // ✏️ 달력은 로딩/에러와 무관하게 항상 그린다(고정 높이). 점 데이터만 응답이 오면
    //   채우고, 로딩/에러 중엔 빈 점으로 둔다 → 아래 목록과 Column으로 붙기 좋다.
    var dayMap = calendarAsync.maybeWhen(
      data: (calendar) => <DateTime, CalendarDay>{
        for (final d in calendar.days) _dateKey(d.date): d,
      },
      orElse: () => <DateTime, CalendarDay>{},
    );

    // [WEB_DEV_ACCESS] 웹 무로그인 dev 모드에서 노트 API가 403이라 점이 안 보일 때,
    // 색점 표시(④)를 눈으로 확인할 수 있도록 샘플 데이터를 채운다. 실데이터가 오면
    // dayMap이 비지 않으므로 목은 사용되지 않는다. 개발 종료 시 web_dev_access.dart와
    // 함께 이 분기를 제거한다.
    if (dayMap.isEmpty && webDevNoLogin) {
      dayMap = _devMockDays(_focusedDay);
    }

    return TableCalendar<CalendarDay>(
      firstDay: DateTime.utc(2020, 1, 1),
      lastDay: DateTime.utc(2035, 12, 31),
      focusedDay: _focusedDay,
      selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
      // ✏️ 점 표시 기준: 그날 saved면 [정보] 반환 → markerBuilder가 카테고리 색점을 찍는다.
      eventLoader: (day) {
        final info = dayMap[_dateKey(day)];
        return (info?.saved ?? false) ? [info!] : [];
      },
      onDaySelected: (selectedDay, focusedDay) =>
          _onDayTap(selectedDay, focusedDay, dayMap),
      // ✏️ 달을 넘기면 _focusedDay를 갱신 → _monthKey 변경 → 그 달 데이터 재조회.
      onPageChanged: (focusedDay) => setState(() => _focusedDay = focusedDay),
      // ⑦ 월/일주일 토글 — 우상단 포맷 버튼으로 전환.
      calendarFormat: _calendarFormat,
      onFormatChanged: (format) => setState(() => _calendarFormat = format),
      availableCalendarFormats: const {
        CalendarFormat.month: 'month',
        CalendarFormat.week: 'week',
      },
      // ⑥ 요일행(일~토)이 잘리지 않도록 높이 확보.
      daysOfWeekHeight: 28,
      availableGestures: AvailableGestures.horizontalSwipe,
      // Calm Paper: 날짜/오늘/선택은 무채색. 저장일 점(marker)은 ④ markerBuilder가
      // 카테고리 색으로 그리므로 calendarStyle.markerDecoration은 적용되지 않는다.
      calendarStyle: CalendarStyle(
        todayDecoration:
            BoxDecoration(color: c.accentSoft, shape: BoxShape.circle),
        todayTextStyle: TextStyle(color: c.text, fontWeight: FontWeight.w600),
        selectedDecoration:
            BoxDecoration(color: c.accent, shape: BoxShape.circle),
        selectedTextStyle:
            TextStyle(color: c.onAccent, fontWeight: FontWeight.w600),
        defaultTextStyle: TextStyle(color: c.text),
        weekendTextStyle: TextStyle(color: c.text),
        outsideTextStyle: TextStyle(color: c.textMuted),
        disabledTextStyle: TextStyle(color: c.textMuted),
      ),
      daysOfWeekStyle: DaysOfWeekStyle(
        weekdayStyle: TextStyle(color: c.text2),
        weekendStyle: TextStyle(color: c.text2),
      ),
      // ④ 카테고리별 색점(최대 3개) — calendarStyle.markerDecoration을 대체한다.
      calendarBuilders: CalendarBuilders<CalendarDay>(
        markerBuilder: (context, day, events) => _buildMarkers(events),
      ),
    );
  }

  /// ④ 카테고리별 색점 빌더 — 정의된 색 순서로 최대 [_kMaxDots]개.
  Widget? _buildMarkers(List<CalendarDay> events) {
    if (events.isEmpty) return null;
    final categories = events.first.categories;
    if (categories.isEmpty) return null;

    // 정의된 색 순서(QT→설교→…)로 정렬하고 중복 제거, 상한 적용.
    final ordered = kNoteCategoryDotColors.keys
        .where(categories.contains)
        .take(_kMaxDots)
        .toList();
    if (ordered.isEmpty) return null;

    return Positioned(
      bottom: 4,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final code in ordered)
            Container(
              width: 5,
              height: 5,
              margin: const EdgeInsets.symmetric(horizontal: 1),
              decoration: BoxDecoration(
                color: kNoteCategoryDotColors[code],
                shape: BoxShape.circle,
              ),
            ),
        ],
      ),
    );
  }

  void _onDayTap(
    DateTime selectedDay,
    DateTime focusedDay,
    Map<DateTime, CalendarDay> dayMap,
  ) {
    setState(() {
      _selectedDay = selectedDay;
      _focusedDay = focusedDay; // 다른 달의 날을 누르면 그 달로 따라감
    });

    // ✏️ 날짜 탭 → 그날 기록 체크리스트(5종, 작성된 카테고리 ✓) 바텀시트. 표시 전용.
    //   작성/이동은 평소 흐름으로 하고, 작성하면 달력이 갱신되며 자동 ✓ 된다.
    showNoteDayChecklistSheet(
      context,
      selectedDay,
      dayMap[_dateKey(selectedDay)],
    );
  }

  // [WEB_DEV_ACCESS] 색점 시각 확인용 샘플 — 포커스 달의 며칠에 다양한 카테고리를 채운다.
  Map<DateTime, CalendarDay> _devMockDays(DateTime month) {
    const samples = <int, List<String>>{
      3: ['MEDITATION'],
      7: ['MEDITATION', 'PRAYER'],
      12: ['SERMON'],
      15: ['MEDITATION', 'SERMON', 'PRAYER', 'GRATITUDE'], // 4종 → 3점으로 잘림
      18: ['GRATITUDE'],
      21: ['REPENTANCE', 'PRAYER'],
      25: ['MEDITATION', 'GRATITUDE'],
    };
    final map = <DateTime, CalendarDay>{};
    samples.forEach((d, cats) {
      final key = DateTime.utc(month.year, month.month, d);
      map[key] = CalendarDay(
        date: key,
        saved: true,
        savedNoteCount: cats.length,
        categories: cats,
      );
    });
    return map;
  }
}
