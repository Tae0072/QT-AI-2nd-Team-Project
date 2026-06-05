# 2026-06-05 TTS 읽기 설정 전용 화면 분리

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다. 제품 반영 시 별도 검토 필요.

## 목표
설정(M-06) 화면에 흩어져 있던 TTS 항목 3개(읽기 목소리, 본문 읽기, 해설 읽기)를
"TTS 읽기 설정" 전용 화면으로 분리해 설정 화면을 단순하게 유지한다.

## 작업 브랜치
`feature/tts-settings-screen` (dev 기반, PR #237 머지 이후 후속)

## 단계
- [x] 1단계: `TtsSettingsScreen` 신설 — 목소리 BottomSheet + 본문/해설 Switch 이동
- [x] 2단계: 설정 화면 단순화 — TTS 섹션을 "TTS 읽기 설정" ListTile 1개로 교체
- [x] 3단계: 라우트 추가 — `AppRouter.ttsSettings = '/settings/tts'`
- [x] 4단계: 위젯 테스트 3건 — 항목 표시 / 해설 토글 저장 / 둘 다 끔 차단
- [x] 5단계: flutter analyze + flutter test (103개) 통과
- [ ] 6단계: 에뮬레이터 화면 흐름 확인 — 에뮬레이터 과부하로 보류 (코드 검증은 테스트로 완료)

## 산출물
| 파일 | 설명 |
|------|------|
| `flutter-app/lib/features/mypage/screens/tts_settings_screen.dart` | TTS 읽기 설정 전용 화면 (신규) |
| `flutter-app/lib/features/mypage/screens/settings_screen.dart` | TTS 섹션 → 진입 ListTile 1개 |
| `flutter-app/lib/routes/app_router.dart` | `/settings/tts` 라우트 |
| `flutter-app/test/features/mypage/screens/tts_settings_screen_test.dart` | 위젯 테스트 3건 (신규) |
