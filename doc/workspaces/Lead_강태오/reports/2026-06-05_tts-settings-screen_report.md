# 2026-06-05 TTS 읽기 설정 전용 화면 분리 결과 보고

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다.

## 요약
설정 화면의 TTS 항목 3개를 "TTS 읽기 설정" 전용 화면(`/settings/tts`)으로 분리했다.
설정 화면에는 진입용 ListTile 1개만 남아 구조가 단순해졌고,
TTS 관련 설정(목소리, 본문/해설 읽기 범위)은 한 화면에서 관리한다.

## 변경 내용
- 신규 `TtsSettingsScreen` (mypage/screens): 읽기 목소리 선택(BottomSheet, 라디오 표시),
  본문 읽기(한글)/해설 읽기 Switch, 둘 다 끔 차단 로직 — 기존 설정 화면에서 그대로 이동
- 설정 화면: TTS 섹션 제거 → "TTS 읽기 설정" ListTile(아이콘 + chevron)로 진입
- 라우터: `AppRouter.ttsSettings = '/settings/tts'` named route 등록

## 변경 파일
| 파일 | 변경 내용 |
|------|----------|
| `flutter-app/lib/features/mypage/screens/tts_settings_screen.dart` | 신규 화면 |
| `flutter-app/lib/features/mypage/screens/settings_screen.dart` | TTS 섹션 → 진입 항목 1개 |
| `flutter-app/lib/routes/app_router.dart` | 라우트 추가 |
| `flutter-app/test/features/mypage/screens/tts_settings_screen_test.dart` | 위젯 테스트 3건 |

## 검증
- [x] flutter analyze: No issues found
- [x] flutter test: 103개 전체 통과 (신규 3건 포함)
  - TTS 읽기 설정 화면이 목소리/본문/해설 항목을 표시한다
  - 해설 읽기 토글을 켜면 SharedPreferences에 저장된다
  - 본문과 해설을 둘 다 끄려고 하면 차단하고 안내한다
- [ ] 에뮬레이터 화면 흐름 — 에뮬레이터 과부하(앱 시작 수 분 지연)로 보류.
  동작 로직은 기존 화면에서 검증된 코드를 그대로 이동했고 위젯 테스트로 커버됨

## 참고
- 빌드 중 `cleanMergeDebugAssets` 디렉터리 잠금 재발 — dart 프로세스 종료 + 수동 삭제로 해결 (반복 패턴)
- 에뮬레이터 장시간 사용 후 시스템 지연(Slow dispatch 2초대) 발생 — 콜드 부팅 권장
