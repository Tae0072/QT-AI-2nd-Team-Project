# 워크플로우 — 노트 펜 색상 선택 + 테마 기본색 / 내 계정 목업 노트

- 날짜: 2026-06-19
- 작성: Lead 강태오 (with Claude)
- 브랜치: `feature/note-pen-color-and-mock` → PR to `dev`
- 도메인: note(기록) + sharing(나눔) — Flutter 앱
- 관련 F-ID: **F-03**(노트/기록 — 손그림 포함), 목업은 **F-03/F-10**(기록/나눔 목록) 표시용 테스트 보조

> 제약: 테마 파일(`app_theme.dart`/`theme_providers.dart`)은 **읽기만** 했고 수정하지 않았다(작업1과 충돌 방지). 펜 기본색은 런타임 `Theme.of(context).brightness`로 읽어 결정한다.

## 작업 (1) 노트 펜 색상 선택 + 테마별 기본색

### 무엇을 바꿨나
- 그동안 펜(손그림) 색은 **텍스트 색(`_textColor`)을 그대로** 썼다. 이를 **펜 전용 색(`_penColor`)으로 분리**했다.
- 펜 버튼을 **길게 누르면(long press)** 펜 색상 선택 시트가 뜬다(짧게 누르면 기존처럼 펜 on/off).
- 펜 기본색은 테마에 맞춘다: **라이트 모드 = 검정(0xFF000000), 다크 모드 = 흰색(0xFFFFFFFF)**. 사용자가 색을 고르면 그 색으로 고정된다.

### 어떻게 (파일별)
- `widgets/qt_note_format_toolbar.dart`
  - `onTogglePenLongPress` 콜백 파라미터 추가.
  - 펜 버튼처럼 **길게 누르기가 필요한 버튼만** `InkWell(onTap+onLongPress)`로 구성. (IconButton 바깥을 GestureDetector로 감싸면 내부 InkWell이 제스처를 가로채 롱프레스가 안 먹는 문제를 피하려고, 탭·롱프레스를 같은 InkWell에 둠.) 일반 버튼은 기존 IconButton 유지.
  - 펜 버튼 툴팁은 `'펜으로 그리기'` 그대로 둠(기존 테스트·UX 계약 유지).
- `widgets/note_rich_text_editor.dart`
  - `Color? _penColor`(null=테마 기본) 상태 추가.
  - `_themeDefaultPenColor()`(라이트 검정/다크 흰색), `_effectivePenColor()`(고른 색 ?? 테마 기본).
  - `_openPenColorSheet()`: 롱프레스 시 펜 색 시트(투명은 무시). 펜 팔레트 `_penColors`(검정·흰색 + 컬러) 추가.
  - 툴바에 `onTogglePenLongPress: _canDraw ? _openPenColorSheet : null` 연결.
  - 손그림 레이어에 `colorValue: _effectivePenColor().toARGB32()` 전달(기존 `_textColor` 대체).

## 작업 (2) 내 계정에 목업(테스트용) 노트

### 방식 결정 (사전 제안 → 사용자 선택)
- **방식: 프론트 임시 목업** (백엔드 시드 대신). DB 무변경 → 운영 데이터 0 오염, 제거 용이.
- **내 계정 식별: 이메일** (`profile.email == 'rkdxodh41@gmail.com'`).
- 기록·나눔 목록은 둘 다 백엔드 API로 로드되므로, **응답을 받은 뒤** 메모리상 목록 맨 앞에 가짜 2개를 끼운다.

### 안전장치 (운영에 절대 안 섞이도록)
1. `kDebugMode`가 아닐 때(릴리스)는 분기 자체가 **컴파일에서 제거**된다(트리 셰이킹).
2. 로그인 이메일이 지정값과 **정확히 같을 때만** 끼운다.
3. **DB 무변경**(메모리 목록 앞에 덧붙이기만). 서버/공개 피드 0 오염.
4. id는 **음수**(-9001 등, 실제와 충돌 불가), 제목에 `[목업]` 표시. 탭 시 상세가 없어 동작 안 할 수 있음(테스트 표시 전용).
5. 목업 로직은 **별도 파일 1개씩**에 격리(`dev_mock_notes.dart`, `dev_mock_sharing.dart`) → 운영 전 파일 삭제 + import 1줄 제거로 끝.

### 어떻게 (파일별)
- `features/note/providers/dev_mock_notes.dart`(신규): 가짜 노트 2개 + `withDebugMockNotes(ref, base)`.
- `features/note/providers/note_providers.dart`: `notesProvider` 두 경로(일반/나눔필터)에 `withDebugMockNotes` 적용.
- `features/sharing/providers/dev_mock_sharing.dart`(신규): 가짜 내 글 2개 + `withDebugMockMySharing(ref, base)`.
- `features/sharing/providers/sharing_providers.dart`: `mySharingPostsProvider`에 적용.

## 검증
- `flutter analyze lib/features/note lib/features/sharing test/features/note` → **No issues found**.
- `flutter test test/features/note test/features/sharing` → **All tests passed (117)**. 펜 롱프레스→색 시트 테스트 1건 추가.
- 테마 파일 미수정 확인(읽기만).

## 주의/후속
- 목업(작업2)은 **테스트 보조**이며 운영 배포 전 제거 대상(파일 2개 + import 2줄). PR에도 명시.
- 작업 트리에 타 작업(테마·알림·admin-web 등)의 미커밋 변경이 섞여 있어, 본 PR은 **노트/나눔 7개 파일 + 본 문서만** 스테이징했다.
