# 2026-06-12 성경 본문 노트 진입점 복원 + 절 범위 선택 (feature/mobile-tabs-layout-and-bible-range)

## 목표·배경
T 요청 2건. ① 성경 본문 화면에 "노트" 버튼이 사라졌다(이전 #505에 있다가 #504 롤백으로 제거됨) → 버튼 복원, 클릭 시 기록(노트) 화면으로 이동(실제 기록은 기록 화면 담당). ② 성경 본문 범위를 지정 못하고 절이 하나씩만 나온다 → 절 범위(시작~끝) 선택 지원.

## 작업 내용
### ① 노트 진입점 복원 (`_BibleTocHeader`)
- 성경 본문 헤더 우측에 "노트" TextButton 추가. 탭 시 `noteCategoryFilterProvider='SERMON'` 설정 후 `AppRouter.noteList`로 이동(설교 노트 맥락 — #505 동작 복원). 실제 작성/저장은 기록 화면(담당)이 처리.

### ② 절 범위 선택
- 원인: 백엔드·상태는 이미 범위 지원(`_verseFrom`/`_verseTo`, `getVerses(verseFrom,verseTo)`, `BiblePassageScreen(range)`). 다만 `_selectVerse`가 항상 `from=to=verse`로 단일만 만들고 UI도 단일만 표시 → "하나씩" 나옴.
- `_selectVerse`를 **앵커 방식**으로: 첫 탭=그 절로 단일 선택(앵커), 둘째 탭=범위 끝 지정(앞/뒤 자동 정렬), 셋째 탭=새 단일 시작. → "탭-탭"이면 범위, "탭" 한 번이면 단일. 책/장 변경·선택 초기화 시 앵커 리셋.
- `verseFrom`/`verseTo`를 `_BibleBrowserContent` → `_BibleTocPicker` → 절 `_BibleNumberColumn(rangeEnd)`까지 전달. 절 열은 `[from..to]` 범위를 accentSoft로 하이라이트(양 끝 w700). 장 열은 단일 그대로(rangeEnd null).
- 선택바(`_BibleSelectionBar`)에 범위 표시: `책 N장 X절` 또는 `책 N장 X–Y절`.

## 범위/주의
- 성경 화면은 이지윤님 담당 도메인. T 지시로 본인이 구현했으며, PR에서 이지윤님 리뷰 필요(도메인 경계 존중).
- "탭 레이아웃을 샘플처럼"의 노트/성경/마이는 dev에 이미 #531/#533/#530으로 Calm Paper 재설계 반영됨 — 중복 재작업 대신 위 2건만 진행.

## 검증
- `flutter analyze` 무이슈, `flutter test test/features/bible` 20건 전체 통과(절 단일 선택 테스트 = 한 번 탭 → 단일 유지 확인).
- 수동: 에뮬레이터(Android API 37) — 성경 헤더 "노트" 버튼 → 기록 화면(설교 칩 선택) 이동 확인. 절 4→8 탭 시 4–8절 하이라이트 + 선택바 "창세기 1장 4–8절" 확인.

담당: DevD 이승욱
