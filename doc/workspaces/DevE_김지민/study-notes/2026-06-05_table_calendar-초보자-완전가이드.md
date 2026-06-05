# table_calendar 초보자 완전 가이드 (설치 → 적용)

> 대상: Flutter 캘린더 처음 쓰는 사람
> 목표: **"저장한 날짜에 체크(✓)/점 표시되는 달력"** 을 우리 묵상 달력에 붙이기
> 설치 버전: `table_calendar: ^3.2.0` (2026-06-05 설치, 의존성으로 `intl`·`simple_gesture_detector` 같이 들어옴)
> 거꾸로 봐도 이해되게 — 한 줄씩, 비유와 함께.

---

## 0. 한 줄 요약 (바쁘면 이것만)

```dart
TableCalendar(
  firstDay: DateTime.utc(2020, 1, 1),   // 달력 시작 한계
  lastDay: DateTime.utc(2030, 12, 31),  // 달력 끝 한계
  focusedDay: _focusedDay,              // 지금 보고 있는 달(필수)
  eventLoader: (day) => 그날저장됐으면[1] else [],   // 점/체크 찍을 날 판단
  onDaySelected: (selected, focused) { /* 날짜 탭 */ },
  onPageChanged: (focused) => _focusedDay = focused, // 월 이동
)
```
- **"날짜에 표시"의 핵심 = `eventLoader`** (그날 뭔가 있으면 리스트 반환 → 달력이 점 찍음)
- 표시를 ✓ 체크나 색깔로 바꾸려면 → `calendarBuilders.markerBuilder`

---

## 1. table_calendar이 뭐야?

Flutter엔 **"월 달력 뷰" 기본 위젯이 없어요.** (날짜 하나 고르는 `showDatePicker`만 있음)
그래서 pub.dev의 **외부 패키지** `table_calendar`를 받아 씁니다. 월 그리드·요일 헤더·이전/다음 달 이동·날짜 표시(마커)를 다 만들어줘요.

비유: 빵을 **밀가루부터 굽는 대신(GridView 직접)**, **완제품 빵을 사 와서(table_calendar)** 우리 토핑(체크 표시)만 얹는 것.

---

## 2. 설치 (이미 완료 — 기록용)

### 2-1. 패키지 추가
터미널에서 (flutter-app 폴더 안에서):
```bash
flutter pub add table_calendar
```
→ `pubspec.yaml`의 `dependencies:`에 자동으로 `table_calendar: ^3.2.0` 추가 + 다운로드까지 됨.

> 직접 적어도 됨: `pubspec.yaml` 열고 `dependencies:` 아래에
> ```yaml
>   table_calendar: ^3.2.0
> ```
> 적은 뒤 `flutter pub get` 실행. (`^3.2.0` = "3.2.0 이상 4.0.0 미만"이라는 뜻)

### 2-2. 쓸 파일 맨 위에 import
```dart
import 'package:table_calendar/table_calendar.dart';
```

> ⚠️ 팀 공용 파일(`pubspec.yaml`)이 바뀌었으니 PR 설명/회의에 "table_calendar 추가" 한 줄 남기기.

---

## 3. 가장 기본 — 달력 그냥 띄우기

캘린더는 **"지금 어느 달을 보고 있는지"(focusedDay)를 기억**해야 해서 `StatefulWidget`이 필요해요.

```dart
import 'package:flutter/material.dart';
import 'package:table_calendar/table_calendar.dart';

class MyCalendar extends StatefulWidget {
  const MyCalendar({super.key});
  @override
  State<MyCalendar> createState() => _MyCalendarState();
}

class _MyCalendarState extends State<MyCalendar> {
  DateTime _focusedDay = DateTime.now(); // 지금 보고 있는 달

  @override
  Widget build(BuildContext context) {
    return TableCalendar(
      firstDay: DateTime.utc(2020, 1, 1),    // 이 날짜보다 과거로는 못 감
      lastDay: DateTime.utc(2030, 12, 31),   // 이 날짜보다 미래로는 못 감
      focusedDay: _focusedDay,               // ★ 필수: 보여줄 달
    );
  }
}
```

### 필수 3총사 외우기
| 속성 | 뜻 | 비유 |
|---|---|---|
| `firstDay` | 갈 수 있는 가장 과거 | 달력의 "왼쪽 끝 벽" |
| `lastDay` | 갈 수 있는 가장 미래 | 달력의 "오른쪽 끝 벽" |
| `focusedDay` | **지금 펼친 달** | 책에서 "지금 편 페이지" |

> 셋 다 안 주면 에러나요. `focusedDay`는 반드시 `firstDay`~`lastDay` 사이에 있어야 함.

---

## 4. 월 이동 (이전/다음 달)

화살표나 스와이프로 달을 넘기면 `onPageChanged`가 불려요. 여기서 `_focusedDay`를 갱신해줘야 화면이 그 달로 바뀝니다.

```dart
TableCalendar(
  firstDay: DateTime.utc(2020, 1, 1),
  lastDay: DateTime.utc(2030, 12, 31),
  focusedDay: _focusedDay,
  onPageChanged: (focusedDay) {
    // ✏️ 달을 넘기면 새 달을 기억. setState 안 해도 table_calendar가 처리하지만,
    //    우리가 "이 달 데이터를 다시 불러와야" 하므로 보관해둔다.
    _focusedDay = focusedDay;
  },
)
```

---

## 5. ★ 핵심 — 날짜에 체크/점 표시하기

여기가 "저장한 날에 표시"의 심장이에요. 방법 2가지.

### 방법 ① eventLoader — 그냥 점(dot) 찍기 (제일 쉬움)
`eventLoader`는 "이 날짜에 일정이 있냐?"를 묻는 함수예요. **리스트를 비어있지 않게 반환하면 달력이 그 날 밑에 점을 자동으로 찍어요.**

```dart
// 저장된 날짜들을 빠르게 찾으려고 Set/Map으로 준비 (3번 참고: API에서 채움)
final Set<DateTime> _savedDays = { /* 저장된 날짜들 */ };

TableCalendar(
  // ...firstDay/lastDay/focusedDay...
  eventLoader: (day) {
    // ✏️ 이 날이 저장된 날이면 [무언가] 반환 → 점이 찍힌다. 아니면 빈 리스트 → 점 없음.
    final key = DateTime.utc(day.year, day.month, day.day); // 시/분 떼고 날짜만
    return _savedDays.contains(key) ? ['저장'] : [];
  },
)
```

> 💡 왜 `DateTime.utc(년,월,일)`로 자르나? → DateTime은 시·분·초까지 들어있어서, `2026-05-17 09:00`과 `2026-05-17 00:00`을 **다른 날로 취급**해요. 날짜만 비교하려면 시간을 떼야 함. (table_calendar의 `isSameDay(a, b)` 헬퍼를 써도 됨)

### 방법 ② markerBuilder — ✓ 체크나 색깔로 직접 그리기
점 대신 **초록 체크(✓)** 를 그리고 싶으면 `calendarBuilders.markerBuilder`를 씀.

```dart
TableCalendar(
  // ...
  eventLoader: (day) => _savedDays.contains(_dateKey(day)) ? ['저장'] : [],
  calendarBuilders: CalendarBuilders(
    markerBuilder: (context, day, events) {
      // events = eventLoader가 그 날 반환한 리스트
      if (events.isEmpty) return null;          // 저장 안 한 날 → 표시 없음
      return const Positioned(                   // 날짜칸 안에 겹쳐 그림
        bottom: 4,
        child: Icon(Icons.check_circle, size: 14, color: Colors.green), // ✓
      );
    },
  ),
)

DateTime _dateKey(DateTime d) => DateTime.utc(d.year, d.month, d.day);
```

> 점 vs 체크: **점이면 ①만으로 충분**, **✓나 카테고리 색이면 ②**.

---

## 6. 날짜 탭 (그 날 노트로 이동)

날짜를 누르면 `onDaySelected`가 불려요. (눌린 날, 현재 포커스 날) 2개를 줌.

```dart
DateTime? _selectedDay;

TableCalendar(
  // ...
  selectedDayPredicate: (day) => isSameDay(_selectedDay, day), // 선택 강조 표시
  onDaySelected: (selectedDay, focusedDay) {
    setState(() {
      _selectedDay = selectedDay;
      _focusedDay = focusedDay; // 다른 달의 날을 누르면 그 달로 따라감
    });
    // ✏️ 여기서 그 날짜의 노트로 이동시키면 됨 (우리 묵상달력: meditationNoteId)
  },
)
```

> `isSameDay`는 table_calendar이 주는 헬퍼 — 시간 무시하고 날짜만 비교해줌. (직접 `==` 비교 금지! 시간 때문에 안 맞음)

---

## 7. ★ 우리 묵상 달력에 적용하기

우리 API `GET /me/meditation-calendar?month=yyyy-MM` 응답:
```json
{
  "month": "2026-05",
  "days": [
    { "date": "2026-05-17", "saved": true, "savedNoteCount": 2,
      "meditationNoteId": 200, "categories": ["MEDITATION","PRAYER"] }
  ],
  "summary": { "savedDays": 10, "savedNoteCount": 12, "meditationStreakDays": 3 }
}
```

### 적용 흐름 (3단계)
1. **API days[] → 빠른 조회용 Map으로 변환**
   ```dart
   // 날짜(시간 뗀 것) → 그 날 정보
   final Map<DateTime, DayInfo> _dayMap = {};
   // 응답 파싱 후:
   for (final d in response.days) {
     final date = DateTime.parse(d.date);            // "2026-05-17"
     _dayMap[DateTime.utc(date.year, date.month, date.day)] = d;
   }
   ```
2. **eventLoader로 "저장한 날" 판단 + markerBuilder로 표시**
   ```dart
   eventLoader: (day) {
     final info = _dayMap[_dateKey(day)];
     return (info?.saved ?? false) ? [info!] : [];   // 저장된 날만 표시
   },
   calendarBuilders: CalendarBuilders(
     markerBuilder: (context, day, events) {
       if (events.isEmpty) return null;
       // 카테고리 개수만큼 색 점, 또는 그냥 ✓ 하나
       return const Icon(Icons.check_circle, size: 12, color: Colors.green);
     },
   ),
   ```
3. **onDaySelected로 그 날 묵상노트로 이동**
   ```dart
   onDaySelected: (selectedDay, focusedDay) {
     final info = _dayMap[_dateKey(selectedDay)];
     if (info?.meditationNoteId != null) {
       Navigator.of(context).pushNamed(
         AppRouter.noteDetail, arguments: info!.meditationNoteId,
       );
     }
   },
   ```
4. **onPageChanged로 달 바뀌면 그 달 데이터 다시 불러오기**
   ```dart
   onPageChanged: (focusedDay) {
     setState(() => _focusedDay = focusedDay);
     // "2026-06" 형태로 만들어 그 달 API 재호출 (provider invalidate 등)
   },
   ```

> 스트릭(🔥 며칠 연속)은 `summary.meditationStreakDays`를 그냥 텍스트로 보여주면 됨 (달력 위/아래에).

---

## 8. 자주 나는 에러 & 주의 (초보 함정)

| 증상 | 원인 | 해결 |
|---|---|---|
| `focusedDay must be ...` 에러 | focusedDay가 firstDay~lastDay 밖 | 범위 안 날짜로 |
| 점이 엉뚱한 날 / 안 찍힘 | DateTime에 시·분이 붙어 비교 실패 | `DateTime.utc(년,월,일)`로 자르거나 `isSameDay` 사용 |
| 날짜 선택해도 강조 안 됨 | `selectedDayPredicate` 안 줌 | `selectedDayPredicate: (d) => isSameDay(_selectedDay, d)` |
| 달 넘겨도 그대로 | `onPageChanged`에서 `_focusedDay` 갱신 안 함 | 거기서 갱신 |
| import 빨간 줄 | pub get 안 됨 | `flutter pub get` 다시 |

---

## 9. 치트시트 (한 장 요약)

```
필수      : firstDay, lastDay, focusedDay
표시(점)  : eventLoader  → 비어있지 않은 리스트 반환하면 점
표시(✓색) : calendarBuilders.markerBuilder
날짜비교  : isSameDay(a, b)  (절대 == 쓰지 말기)
날짜키    : DateTime.utc(y, m, d)  (시간 떼기)
탭       : onDaySelected (selected, focused)
월이동    : onPageChanged (focused)
선택강조  : selectedDayPredicate
```

---

## 참고
- 패키지: https://pub.dev/packages/table_calendar
- 우리 API: `doc/standards/04_API_명세서.md` §4.6.2 묵상 달력
- 적용 위치(예정): `flutter-app/lib/features/note/` (N-01 달력 토글)
