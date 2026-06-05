# 2026-06-05 TTS 목소리 설정 + 자동 음성 생성 결과 보고

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다.

## 요약
TTS QT 읽기 기능의 8~10단계를 완료했다.
목소리 선택을 마이페이지 설정으로 옮겨 SharedPreferences에 영구 저장하고,
QT 본문 로드 시 백그라운드로 음성을 미리 생성해 ▶ 버튼으로 즉시 재생되도록 했다.

## 완료된 작업

### 1. 마이페이지 설정에 TTS 목소리 선택 (8단계)
- `selectedVoiceProvider`를 StateProvider → `StateNotifierProvider`로 교체
- `SelectedVoiceNotifier`: 선택 시 `tts_selected_voice` 키로 SharedPreferences 저장,
  앱 재시작 후에도 유지 (main에서 주입되는 `sharedPreferencesProvider` 재사용)
- 설정 화면(M-06)에 "QT 읽기 목소리" 항목 추가 — BottomSheet에서
  `/voices` 목록 선택, 현재 선택은 라디오 아이콘으로 표시

### 2. QT 본문 로드 시 자동 음성 생성 (9단계)
- `QtAudioPlayer.initState`의 `addPostFrameCallback`에서 `_prepareAudio()` 호출
- QT 화면이 뜨면 사용자 조작 없이 백그라운드에서 음성 생성(또는 캐시 로드) 시작
- 캐시 키 `{날짜}_{목소리해시}` 유지 — 같은 날짜·목소리는 재생성하지 않음

### 3. QT 화면에서 바로 재생 (10단계)
- ▶ 버튼: 준비된 음성이 있으면 즉시 재생, 없으면(준비 실패 등) 생성 후 자동 재생
- `ref.listen(selectedVoiceProvider)`: 설정에서 목소리 변경 시 정지 후 새 목소리로 재준비
- 플레이어 내 목소리 선택 버튼 제거 → 현재 목소리 표시 전용

## 변경 파일
| 파일 | 변경 내용 |
|------|----------|
| `flutter-app/lib/features/tts/providers/tts_providers.dart` | SelectedVoiceNotifier + SharedPreferences 영구 저장 |
| `flutter-app/lib/features/mypage/screens/settings_screen.dart` | QT 읽기 목소리 설정 UI (BottomSheet) |
| `flutter-app/lib/features/tts/widgets/qt_audio_player.dart` | 자동 사전 생성 + 즉시 재생 + 목소리 변경 감지 |
| `doc/workspaces/Lead_강태오/workflows/2026-06-04_tts-qt-reader-integration.md` | 8~10단계 완료 갱신 |

## 구현 시 고려한 점
- 생성 중 중복 호출 방지: `_isGenerating` 가드 (생성 중에는 ▶ 자리에 스피너 표시)
- 비동기 완료 후 `mounted` 확인 — 화면 이탈 후 setState 방지
- 설정 화면에 머무는 동안 QT 화면이 살아 있으면 `ref.listen`으로 즉시 재준비,
  화면이 다시 만들어지면 `initState`에서 새 목소리로 준비 — 두 경로 모두 커버
- TTS 토큰 미설정 시 기존과 동일하게 안내 메시지 표시

## 검증
- [x] flutter analyze: No issues found (40.3s)
- [x] 코드 2차 검토: 중복 호출 가드, mounted 가드, 목소리 변경 양쪽 경로 확인
- [ ] 에뮬레이터 동작 확인 — hot restart(R) 필요 (initState 변경은 hot reload로 반영 안 됨)

## 다음 작업 후보
- 에뮬레이터 E2E 확인 후 PR 정리
- 자동 생성 실패 시 재시도 정책 (현재는 ▶ 누르면 재시도)
- 커스텀 목소리 학습 상태를 설정 화면에 표시
