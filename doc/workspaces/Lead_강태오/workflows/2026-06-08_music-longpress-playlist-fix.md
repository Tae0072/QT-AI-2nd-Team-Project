# 2026-06-08 · 배경음악 음표 버튼 "길게 누르기" 음원 목록 미동작 수정 — 워크플로우

작성: Claude (Lead 강태오/T 지시) · 대상 저장소: `QT-AI-2nd-Team-Project` (구현, Flutter)
관련 기능: 2026-06-07 앱 전역 배경음악(`domain.music`) 후속 버그 수정

## 1. 요약 (한 줄)

오늘의 QT 앱바의 음표 버튼을 **길게 누르면** 음원 목록 시트가 떠야 하는데 안 뜨는 버그를 고친다. (탭으로 켜기/끄기는 정상.)

## 2. 증상

- 음표 버튼 **짧게 탭 → 배경음악 켜기/끄기**: 정상 동작.
- 음표 버튼 **길게 누르기 → 음원 목록(재생목록) 시트**: 동작 안 함. 대신 "배경음악 끄기 (길게: 목록)" **툴팁만** 뜬다.
- 사용자 제보 스크린샷에서 길게 누른 직후 툴팁이 떠 있는 게 그대로 보인다 → 길게 누르기가 시트가 아니라 툴팁으로 가고 있다는 결정적 단서.

## 3. 근본 원인

파일: `flutter-app/lib/features/music/widgets/music_toggle_button.dart`

수정 전 구조:

```dart
GestureDetector(
  onLongPress: () => _showTrackSheet(context),   // 우리가 원하는 동작
  child: IconButton(
    tooltip: '배경음악 끄기 (길게: 목록)',          // ← 문제의 원인
    onPressed: ...,                               // 탭 = 켜기/끄기
  ),
)
```

- Flutter의 `IconButton(tooltip: ...)`는 내부에 `Tooltip` 위젯을 만든다.
- **모바일에서 `Tooltip`의 기본 표시 트리거가 "길게 누르기(longPress)"** 이다.
- 따라서 길게 누르면, 자식인 `Tooltip`의 길게 누르기 인식기가 **제스처 경합(gesture arena)** 에서 부모 `GestureDetector.onLongPress`보다 먼저 이겨서 **툴팁만 표시**되고, 시트를 여는 `onLongPress`는 무시된다.
- 탭(`onPressed`)은 길게 누르기와 경합하지 않으므로 정상 → "켜고 끄는 건 됨"과 정확히 일치.

요약: **하나의 위젯에서 "길게 누르기"가 툴팁과 음원 목록 두 가지를 동시에 의미할 수 없어서** 충돌한 것.

## 4. 해결 방법

길게 누르기를 음원 목록 시트에 양보하고, 툴팁이 길게 누르기를 가로채지 못하게 한다.

- `IconButton`의 `tooltip:` 인자를 제거(= 길게 누르기를 훔쳐가던 내부 Tooltip 제거).
- 버튼 전체를 `Tooltip(triggerMode: TooltipTriggerMode.manual)`로 감싼다.
  - `manual` 트리거는 길게 누르기/탭 제스처로 툴팁을 띄우지 않는다 → 길게 누르기가 그대로 `GestureDetector.onLongPress`로 전달됨.
  - 데스크톱/웹에서는 마우스 호버 힌트로 툴팁이 그대로 보인다(2026-06-07 flutter-web-run-support 고려).
- 결과: 길게 누르기의 유일한 인식기가 `GestureDetector.onLongPress` 하나뿐 → 시트가 안정적으로 열린다. 탭(켜기/끄기)은 그대로.

대안으로 `InkResponse(onTap+onLongPress)` 단일 위젯도 가능하나, 기존 `IconButton`의 외형/접근성/최소 터치영역(48dp)을 유지하기 위해 **최소 변경**(tooltip 트리거만 교체)을 택했다. (CLAUDE.md §9: 관련 없는 대규모 리팩터링 금지)

## 5. 변경 파일

- `flutter-app/lib/features/music/widgets/music_toggle_button.dart` (수정)
- `flutter-app/test/features/music/widgets/music_toggle_button_test.dart` (신규, 회귀 테스트)
- 본 워크플로우/리포트 문서 2종

## 6. 검증 계획

- 신규 위젯 테스트 2건:
  1. 길게 누르기 → `MusicTrackSheet`가 열리고 헤더 '배경음악'이 보인다.
  2. 버튼을 감싼 `Tooltip.triggerMode == manual` (재발 방지 가드).
- 수동 확인(안드로이드 실기/에뮬레이터): 탭=켜기/끄기, 길게=목록 시트.
- 명령: `flutter test test/features/music/widgets/music_toggle_button_test.dart` 및 `flutter analyze` (Windows).

## 7. Git (제안)

- 작업 브랜치: `dev`에서 분기 → `fix/ui-music-longpress-playlist`
- 커밋(Conventional Commits): `fix(music): 음표 버튼 길게 누르기 시 음원 목록 시트 표시 (tooltip이 longpress 가로채던 문제)`
- PR 대상: `dev` (10 files / 500 lines 이하 — 본 변경은 소규모)

## 8. 참고/주의

- master에는 아직 `flutter-app`이 없다(`.gitkeep`만 존재) → 이 수정은 dev 라인 대상.
- (별건) 같은 실행 로그에서 `table_calendar`의 `FormatButton`이 `onFormatChanged` 없이 사용되어 길게 누르기 시 assertion 예외가 보인다. 이번 음악 버그와 무관하나 별도 확인 권장.
