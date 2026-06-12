# 결과 리포트 — 노트 목록 다중 선택 삭제 + QT/설교 FAB 숨김 (①②)

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김지민 (flutter-app) |
| 브랜치 | `feature/note-list-fab-and-multidelete` (base `dev`) |
| 관련 F-ID | F-03 · F-16 · F-13 |
| workflow | doc/workspaces/DevE_김지민/workflows/2026-06-12_노트목록-다중삭제-QT설교FAB숨김-워크플로우.md |
| 상태 | 구현·테스트·리뷰 보강 완료, PR 진행 중 |

## 한 일

### ① 노트 목록 다중 선택 삭제 (N-01)
- AppBar **☰(선택)** → 선택 모드(**✕ + "n개 선택" + 전체선택** 토글). 카드 좌측 체크 + 선택 강조.
- 하단 **삭제 바**(선택 0개 비활성) → 확인 다이얼로그 → `deleteMany` → 목록·달력 invalidate → 모드 종료
  → 결과 스낵바.
- `noteSelectionMode`/`noteSelectedIds` provider, `NoteRepository.deleteMany`(단건 DELETE 반복, 실패 id 반환),
  `NoteCard` 선택 props 추가.

### ② QT/설교 칩 작성 FAB 숨김 + 빈 상태 안내
- `showFab = !selectionMode && !tabAuthoredCategories.contains(category)` → QT/설교 칩·선택 모드에서 작성 FAB 숨김.
- 빈 목록 시 QT→"오늘의 QT 화면에서 작성", 설교→"성경 화면에서 작성" 안내.

## 리뷰 반영 (2차)
- **선택 누수 차단(데이터 손실 방지):** 삭제 시점에 **현재 보이는 목록과 교집합** + **필터 변경 시 선택 초기화**(`ref.listen`).
- **catch 범위 축소:** `deleteMany`를 `on DioException`으로 좁혀 네트워크/HTTP만 부분 실패로 모으고 그 외는 전파.
- **부분 성공 메시지 정확화:** `noteDeletePartial(ok, failed)` = "{ok}개 삭제, {failed}개 실패".
- **카테고리 매직 스트링 제거:** `kNoteCatMeditation/Sermon` + `tabAuthoredCategories` 상수화.
- **테스트 보강:** deleteMany(repository 성공/실패) · 전체선택 토글 · 필터 변경 선택 초기화 · 빈 상태 안내 · 삭제 취소 · 부분 실패 스낵바.

## 검증
- `flutter analyze` 클린 + `flutter test` 전체 **228건 통과**.

## 남은 리스크 / 후속
- 선택 모드 상태가 글로벌 provider라 탭 전환 후 유지 가능(✕로 해제) — 후속에서 화면 이탈 시 리셋 검토.
- 삭제는 단건 DELETE 반복(서버 bulk 없음) — 대량 선택 시 bulk API 개선 여지.
- 빈 상태 안내는 텍스트 유도까지(탭 이동 버튼 미구현 — 탭 전환 결합 회피).
