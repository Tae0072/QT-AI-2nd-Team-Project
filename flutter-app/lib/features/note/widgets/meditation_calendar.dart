import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:table_calendar/table_calendar.dart';

import '../../../core/theme/app_theme.dart';
import '../models/note_models.dart';
import '../providers/note_providers.dart';
import 'note_day_checklist_sheet.dart';

/// 묵상 달력 (N-01의 달력 모드 상단).
///
/// - GET /me/meditation-calendar?month=yyyy-MM 을 월별로 조회(meditationCalendarProvider)
/// - 저장된 날에 점(dot) 표시. 날짜 탭 시 그날 기록 체크리스트 바텀시트(표시 전용).
/// - 이전/다음 달로 넘기면 그 달을 다시 조회.
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

  // "yyyy-MM" — provider/API에 넘길 월 키.
  String get _monthKey =>
      '${_focusedDay.year}-${_focusedDay.month.toString().padLeft(2, '0')}';

  // ✏️ DateTime은 시·분이 붙어 비교가 어긋나므로, 비교용 키는 날짜만 남긴다.
  DateTime _dateKey(DateTime d) => DateTime.utc(d.year, d.month, d.day);

  @override
  Widget build(BuildContext context) {
    final calendarAsync = ref.watch(meditationCalendarProvider(_monthKey));
    // ✏️ 달력은 로딩/에러와 무관하게 항상 그린다(고정 높이). 점 데이터만 응답이 오면
    //   채우고, 로딩/에러 중엔 빈 점으로 둔다 → 아래 목록과 Column으로 붙기 좋다.
    final dayMap = calendarAsync.maybeWhen(
      data: (calendar) => <DateTime, CalendarDay>{
        for (final d in calendar.days) _dateKey(d.date): d,
      },
      orElse: () => <DateTime, CalendarDay>{},
    );

    final c = context.appColors;
    return TableCalendar<CalendarDay>(
      firstDay: DateTime.utc(2020, 1, 1),
      lastDay: DateTime.utc(2035, 12, 31),
      focusedDay: _focusedDay,
      selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
      // ✏️ 점 표시 기준: 그날 saved면 [정보] 반환 → 달력이 점을 찍는다. 아니면 빈 리스트.
      eventLoader: (day) {
        final info = dayMap[_dateKey(day)];
        return (info?.saved ?? false) ? [info!] : [];
      },
      onDaySelected: (selectedDay, focusedDay) =>
          _onDayTap(selectedDay, focusedDay, dayMap),
      // ✏️ 달을 넘기면 _focusedDay를 갱신 → _monthKey 변경 → 그 달 데이터 재조회.
      onPageChanged: (focusedDay) => setState(() => _focusedDay = focusedDay),
      calendarFormat: CalendarFormat.month,
      availableGestures: AvailableGestures.horizontalSwipe,
      // Calm Paper: 달력도 무채색만. 저장일 점(marker)만 유일한 유채색(accentDot).
      calendarStyle: CalendarStyle(
        todayDecoration: BoxDecoration(color: c.accentSoft, shape: BoxShape.circle),
        todayTextStyle: TextStyle(color: c.text, fontWeight: FontWeight.w600),
        selectedDecoration: BoxDecoration(color: c.accent, shape: BoxShape.circle),
        selectedTextStyle:
            TextStyle(color: c.onAccent, fontWeight: FontWeight.w600),
        markerDecoration:
            BoxDecoration(color: c.accentDot, shape: BoxShape.circle),
        markersMaxCount: 1,
        defaultTextStyle: TextStyle(color: c.text),
        weekendTextStyle: TextStyle(color: c.text),
        outsideTextStyle: TextStyle(color: c.textMuted),
        disabledTextStyle: TextStyle(color: c.textMuted),
      ),
      daysOfWeekStyle: DaysOfWeekStyle(
        weekdayStyle: TextStyle(color: c.text2),
        weekendStyle: TextStyle(color: c.text2),
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
}
