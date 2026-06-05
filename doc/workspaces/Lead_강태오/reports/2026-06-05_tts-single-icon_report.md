# 2026-06-05 TTS 플레이어 카드 제거 + 앱바 단일 아이콘 결과 보고

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다.

## 요약
QT 화면의 TTS 플레이어 카드(시크바·시간·목소리 라벨)를 전부 제거하고,
앱바의 뒤로가기/새로고침과 같은 **단일 아이콘 버튼**으로 단순화했다.
카드 내부 Row가 좁은 폭에서 일으키던 화면 오버플로우(터짐)도 함께 해소됐다.

## 변경 내용
- `QtAudioPlayer`(카드형) → `QtTtsButton`(아이콘형)으로 교체,
  파일도 `qt_audio_player.dart` → `qt_tts_button.dart`로 이동
- 배치: 오늘 QT 앱바 actions (TTS 아이콘 → 새로고침 순)
- 동작 (아이콘 하나로 모두 제어)
  - 본문 로드 시: 백그라운드 자동 음성 준비 (앱바에 작은 스피너)
  - 탭: 즉시 재생 (미준비 시 생성 후 자동 재생) — 아이콘 빨간색
  - 재생 중 탭: 정지 + 처음으로 — 아이콘 원래 색
  - 끝까지 재생되면 자동으로 정지 상태 복귀
  - 목소리 변경(설정) 시 자동 재준비, 오류는 SnackBar 안내
- 본문 텍스트/날짜 추출 헬퍼를 `today_qt_screen.dart` 상단으로 이동
  (앱바에서 사용하기 위해 `passage.valueOrNull` 기반으로 변경)

## 변경 파일
| 파일 | 변경 내용 |
|------|----------|
| `flutter-app/lib/features/tts/widgets/qt_tts_button.dart` | 단일 아이콘 TTS 버튼 (구 qt_audio_player.dart) |
| `flutter-app/lib/features/bible/screens/today_qt_screen.dart` | 카드 제거, 앱바에 QtTtsButton 배치 |

## 검증
- [x] flutter analyze: No issues found
- [x] 에뮬레이터 E2E (adb 스크린샷)
  - 본문 화면에서 플레이어 카드 제거 확인, 오버플로우(노란 줄) 없음
  - 앱바에 자동 생성 스피너 → 완료 후 아이콘 표시
  - 탭 → 재생(아이콘 빨강) → 재탭 → 정지(원래 색) 토글 정상
