# 2026-06-08 · 배경음악 음표 버튼 "길게 누르기" 미동작 수정 — 리포트

작성: Claude (Lead 강태오/T 지시) · 저장소: `QT-AI-2nd-Team-Project` (Flutter)
워크플로우: `workflows/2026-06-08_music-longpress-playlist-fix.md`

## 1. 결과 요약

오늘의 QT 음표 버튼 **길게 누르기 → 음원 목록 시트** 가 열리지 않던 버그를 수정했다. 원인은 `IconButton(tooltip:)`이 모바일에서 길게 누르기를 가로채(툴팁 기본 트리거 = longPress) 우리의 `onLongPress`보다 먼저 동작한 것. 툴팁을 `manual` 트리거로 바꿔 길게 누르기를 시트에 양보했다. 탭(켜기/끄기)은 영향 없음.

## 2. 변경 내용

### 2.1 `lib/features/music/widgets/music_toggle_button.dart`

- `IconButton`의 `tooltip:` 인자 제거.
- 버튼을 `Tooltip(message: ..., triggerMode: TooltipTriggerMode.manual)`로 감쌈.
- `GestureDetector.onLongPress` → `_showTrackSheet`는 유지(이제 유일한 longPress 인식기).
- 왜 이렇게 고쳤는지 코드 주석으로 남김(재발 방지).

### 2.2 `test/features/music/widgets/music_toggle_button_test.dart` (신규)

| 테스트 | 검증 |
|---|---|
| 길게 누르면 음원 목록 시트가 열린다 | longPress 후 `MusicTrackSheet` + 헤더 '배경음악' 표시 |
| Tooltip은 manual 트리거여야 한다 | `Tooltip.triggerMode == manual` (재발 가드) |

- 기존 `today_qt_screen_test`와 동일하게 `musicControllerProvider` override 없이 동작(길게 누르기 경로는 상태 읽기만 하므로 `just_audio` 플랫폼 호출 없음).

## 3. 검증

| 항목 | 상태 | 비고 |
|---|---|---|
| 정적 검토(코드 리뷰) | ✅ 3회 | 아래 4절 |
| 변경 파일 컴파일 영향 | ✅ | `Tooltip`/`TooltipTriggerMode`는 `material` 기본 제공, import 추가 불필요 |
| 위젯 테스트 작성 | ✅ | 2건 추가 |
| `flutter test` 실제 실행 | ⏳ 사용자(Windows)에서 실행 필요 | 샌드박스에 Flutter 미설치 |
| 안드로이드 실기 확인 | ⏳ 사용자 확인 권장 | 탭=토글, 길게=목록 |

권장 실행(사용자 PC):

```bash
cd flutter-app
flutter test test/features/music/widgets/music_toggle_button_test.dart
flutter analyze
```

## 4. 2~3회 재검토 노트

1. **1차(원인)**: 스크린샷에 툴팁이 떠 있음 → 길게 누르기가 시트가 아니라 툴팁으로 라우팅됨을 확인. `dev` 브랜치 동일 코드 확인. 탭 정상/길게 비정상이 "툴팁이 longPress만 가로챔"과 일치.
2. **2차(수정 정확성)**: `IconButton`은 `onLongPress` 인자가 없어 외부 제스처가 필요. tooltip 제거로 경합이 사라져 `GestureDetector.onLongPress`가 단독으로 동작. 탭 vs 길게 누르기 경합은 Flutter 표준대로 안정적.
3. **3차(부작용)**: `manual` 툴팁은 호버(데스크톱/웹)에선 그대로 표시, 모바일에선 길게 누르기를 가로채지 않음. 최소 변경으로 외형/접근성 유지. 다른 화면/위젯 영향 없음(단일 위젯 국소 변경).

## 5. 후속/주의

- Git: `dev`에서 `fix/ui-music-longpress-playlist` 분기 → 커밋 `fix(music): ...` → PR 대상 `dev`.
- (별건, 무관) 실행 로그에 `table_calendar` `FormatButton`의 `onFormatChanged` 미지정 assertion 예외가 있음 → 기록탭/달력 화면 별도 점검 권장.
- master에는 아직 `flutter-app` 미존재(`.gitkeep`).
