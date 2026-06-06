import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:table_calendar/table_calendar.dart';

import 'package:qtai_app/l10n/app_localizations.dart';
import '../../../core/widgets/common_widgets.dart';
import '../../../routes/app_router.dart';
import '../models/note_models.dart';
import '../providers/note_providers.dart';

/// 묵상 달력 (N-01의 달력 모드).
///
/// - GET /me/meditation-calendar?month=yyyy-MM 을 월별로 조회(meditationCalendarProvider)
/// - 저장된 날에 점(dot) 표시(① 방식). 나중에 markerBuilder로 ✓/색 점으로 교체 쉬움.
/// - 날짜 탭 시 그날 묵상노트(meditationNoteId)가 있으면 N-04 상세로 이동
/// - 이전/다음 달로 넘기면 그 달을 다시 조회
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
    // ✏️ 보고 있는 달(_monthKey)로 달력 데이터를 watch. 달을 넘기면 키가 바뀌어 자동 재조회.
    final calendarAsync = ref.watch(meditationCalendarProvider(_monthKey));

    return calendarAsync.whenOrDefault(
      data: (calendar) {
        // ✏️ 빠른 조회를 위해 days[]를 "날짜키 → 그날 정보" Map으로 바꾼다.
        final dayMap = <DateTime, CalendarDay>{
          for (final d in calendar.days) _dateKey(d.date): d,
        };

        return Column(
          children: [
            TableCalendar<CalendarDay>(
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
              onPageChanged: (focusedDay) =>
                  setState(() => _focusedDay = focusedDay),
              calendarFormat: CalendarFormat.month,
              availableGestures: AvailableGestures.horizontalSwipe,
            ),
            const SizedBox(height: 8),
            _SummaryBar(summary: calendar.summary),
          ],
        );
      },
    );
  }

  Future<void> _onDayTap(
    DateTime selectedDay,
    DateTime focusedDay,
    Map<DateTime, CalendarDay> dayMap,
  ) async {
    setState(() {
      _selectedDay = selectedDay;
      _focusedDay = focusedDay; // 다른 달의 날을 누르면 그 달로 따라감
    });

    // ✏️ 그날 묵상노트가 있으면 상세로 이동. 돌아오면(삭제/수정 가능성) 이 달을 다시 조회.
    final info = dayMap[_dateKey(selectedDay)];
    final noteId = info?.meditationNoteId;
    if (noteId == null) return;

    await Navigator.of(context)
        .pushNamed(AppRouter.noteDetail, arguments: noteId);
    if (!mounted) return;
    ref.invalidate(meditationCalendarProvider(_monthKey));
  }
}

/// 달력 아래 요약 줄(저장한 날 수 · 연속 묵상일).
class _SummaryBar extends StatelessWidget {
  final CalendarSummary summary;

  const _SummaryBar({required this.summary});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l = AppLocalizations.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          Text(l.calSavedThisMonth(summary.savedDays),
              style: theme.textTheme.bodyMedium),
          Text(l.calNoteCount(summary.savedNoteCount), style: theme.textTheme.bodyMedium),
          Text(l.calStreak(summary.meditationStreakDays),
              style: theme.textTheme.bodyMedium),
        ],
      ),
    );
  }
}
