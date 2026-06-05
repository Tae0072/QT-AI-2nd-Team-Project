# 2026-06-05 TTS 버튼 커스텀 아이콘 + 재생/정지 토글 결과 보고

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다.

## 요약
QT 화면의 TTS 시작 버튼을 요청받은 커스텀 아이콘(원 안의 얼굴 옆모습 + 음파 2줄)으로
교체하고, 단일 버튼 토글로 동작을 단순화했다: 탭 → 재생, 다시 탭 → 정지.

## 완료된 작업

### 1. 커스텀 아이콘 에셋 제작
- 참고 이미지가 파일로 전달되지 않아 PIL(Pillow)로 동일 컨셉을 직접 재현
  (링 + Catmull-Rom 곡선 얼굴 옆모습 + 동심 음파 호 2개)
- 검정 + 투명 배경 알파 마스크 PNG(512px) → Flutter `ImageIcon`이
  알파를 마스크로 사용해 테마 색을 자동 적용
- 생성 스크립트: `flutter-app/tool/make_tts_voice_icon.py` (재현 가능)
- 에셋: `flutter-app/assets/icons/tts_voice.png`, pubspec `assets/icons/` 등록

### 2. 재생/정지 토글
- 기존 ▶/⏸ + 정지(■) 2버튼 → 커스텀 아이콘 단일 버튼으로 통합
- 동작: 정지 상태 탭 → 즉시 재생(미준비 시 생성 후 자동 재생),
  재생 중 탭 → 정지 + 처음으로 되감기
- 상태 표시: 정지 상태 = primary(초록) 배경, 재생 중 = error(빨강) 배경
- tooltip: "TTS 시작" / "TTS 정지"

## 변경 파일
| 파일 | 변경 내용 |
|------|----------|
| `flutter-app/assets/icons/tts_voice.png` | 커스텀 TTS 아이콘 (알파 마스크) |
| `flutter-app/tool/make_tts_voice_icon.py` | 아이콘 생성 스크립트 |
| `flutter-app/pubspec.yaml` | assets/icons/ 등록 |
| `flutter-app/lib/features/tts/widgets/qt_audio_player.dart` | 단일 토글 버튼 + ImageIcon |

## 검증
- [x] flutter analyze: No issues found
- [x] 아이콘 가독성: 64/48/32px 렌더링 확인
- [x] 에뮬레이터 E2E (adb 스크린샷)
  - 앱 재시작 후에도 목소리 설정(인준) 유지 — SharedPreferences 영구 저장 재확인
  - QT 진입 → 자동 생성 → 아이콘 버튼(초록) 표시
  - 탭 → 즉시 재생 + 버튼 빨강 전환 (0:03 진행 확인)
  - 다시 탭 → 정지 + 초기화 + 버튼 초록 복귀
  - 재탭 → 처음부터 재생 (캐시 즉시 로드, 0:02 진행 확인)

## 참고
- 정지 후에는 플레이어가 idle로 돌아가 시크바가 접힌다(컴팩트 표시).
  다시 탭하면 캐시에서 즉시 재생된다.
- Gradle `cleanMergeDebugAssets` 잠금 실패 재발 → dart 프로세스 종료 +
  `build/app/intermediates/assets` 수동 삭제로 해결 (반복 시 빌드 전 정리 스크립트 고려)
