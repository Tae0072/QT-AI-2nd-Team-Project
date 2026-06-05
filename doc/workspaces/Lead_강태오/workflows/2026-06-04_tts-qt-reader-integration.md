# 2026-06-04 TTS QT 읽기 기능 Flutter 앱 통합

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다. 제품 반영 시 별도 검토 필요.

## 목표
Voice Studio TTS 엔진을 QT-AI Flutter 앱에 통합하여,
오늘의 QT 본문을 음성으로 읽어주는 기능을 구현한다.

## 작업 브랜치
`feat/tts-qt-reader` (dev 기반, PR은 완성 후 일괄 제출)

## 단계

### 1단계: TTS API QT 전용 엔드포인트 (완료)
- `POST /qt/read` 엔드포인트 추가 (`tts_api.py`)
- 긴 QT 본문 자동 분할 → 개별 생성 → 합치기
- WAV/MP3 출력 선택 (MP3 시 ffmpeg 변환, 용량 약 1/10)
- 커스텀 목소리: GPT-SoVITS → OpenVoice 폴백 → Edge TTS 순
- Bearer 토큰 인증 적용

### 2단계: TTS API 서버 배포 구성
- 상시 실행 스크립트 (VBS 또는 systemd)
- GPU PC: 전체 기능 (커스텀 목소리 포함)
- CPU 서버: Edge TTS만 (GPU 불필요)
- CORS 설정 (Flutter 앱에서 접근 허용)

### 3단계: Flutter TTS 서비스 클래스
- `flutter-app/lib/features/tts/services/tts_service.dart`
- Dio 클라이언트로 `/qt/read` 호출
- 음성 파일 다운로드 → 로컬 캐시 저장
- TTS 서버 URL 설정 (AppConfig에 ttsBaseUrl 추가)

### 4단계: Flutter 오디오 플레이어 위젯
- `just_audio` 패키지 사용
- 재생/일시정지/정지 + 시크바 + 시간 표시
- 하단 미니 플레이어 또는 QT 화면 내장

### 5단계: QT 화면에 '읽어주기' 버튼 연동
- QT 상세 화면에 버튼 추가
- 목소리 선택 (Edge TTS 기본 + 커스텀)
- 생성 중 로딩 표시

### 6단계: 캐싱
- 같은 QT 본문+목소리 조합은 한 번만 생성
- 로컬 캐시 키: `{날짜}_{voice_hash}.mp3`

### 7단계: E2E 테스트 + 문서화

## 기술 스택
| 역할 | 라이브러리 |
|------|-----------|
| TTS API | FastAPI (Python) |
| 음성 엔진 | Edge TTS + OpenVoice + GPT-SoVITS |
| Flutter HTTP | Dio |
| Flutter 오디오 | just_audio |
| 상태 관리 | Riverpod |

## 현재 진행 상태
- [x] 1단계: QT 전용 엔드포인트 추가 — `POST /qt/read`
- [x] 2단계: 서버 배포 구성 — CORS, AppConfig ttsBaseUrl, 웹 호환(kIsWeb)
- [x] 3단계: Flutter TTS 서비스 — `TtsRepository` + Riverpod providers
- [x] 4단계: 오디오 플레이어 위젯 — `QtAudioPlayer` (just_audio)
- [x] 5단계: QT 화면 연동 — `today_qt_screen.dart`에 QtAudioPlayer 배치
- [x] 6단계: 캐싱 — cacheKey 기반 로컬 파일 캐싱
- [x] 7단계: E2E 환경 구성 — MySQL(qtai/qtai) + Redis(Docker) + JWT + 서버 기동 성공
- [x] 8단계: 마이페이지 설정에 TTS 목소리 선택 — SharedPreferences 영구 저장 (06-05)
- [x] 9단계: QT 본문 로드 시 자동 음성 생성 — QtAudioPlayer initState에서 사전 준비 (06-05)
- [x] 10단계: QT 화면에서 바로 재생 — 준비된 음성 즉시 플레이, 목소리 변경 시 자동 재준비 (06-05)
- [x] 11단계: TTS 버튼 커스텀 아이콘 + 재생/정지 토글 — 얼굴+음파 아이콘, 탭=재생/다시 탭=정지 (06-05)
- [x] 12단계: 플레이어 카드 제거 → 앱바 단일 아이콘(QtTtsButton)으로 단순화, Row 오버플로우 해소 (06-05)

## 8~10단계 설계 (2026-06-05)
- `selectedVoiceProvider`: StateProvider → `StateNotifierProvider`(SelectedVoiceNotifier)로 교체.
  `sharedPreferencesProvider`(main에서 주입)를 통해 `tts_selected_voice` 키로 영구 저장
- 설정 화면(M-06): "QT 읽기 목소리" ListTile + BottomSheet 선택 UI 추가
  (라디오 표시로 현재 선택 강조, `/voices` 목록 사용)
- `QtAudioPlayer`: 위젯 표시 직후 `addPostFrameCallback`으로 `_prepareAudio()` 호출 →
  본문 로드 시 백그라운드 자동 생성. ▶ 버튼은 준비된 음성 즉시 재생
- `ref.listen(selectedVoiceProvider)`: 설정에서 목소리 변경 시 정지 후 새 목소리로 재준비
- 플레이어 내 목소리 선택 버튼 제거 → 현재 목소리 표시 전용 (변경은 설정에서)

## E2E 테스트 환경 구성 기록
- MySQL root 비밀번호: Workbench 키체인 저장 (cmd `0000` 아님)
- qtai DB/사용자: Workbench에서 SQL 실행하여 생성 (`qtai`/`qtai`)
- Redis: Docker `qtai-redis` 컨테이너 (`docker start qtai-redis`)
- JWT 키: `src/test/resources/application.yml`에서 PowerShell 환경변수로 주입
- qtai-server: `.\gradlew bootRun --args="--spring.profiles.active=dev"` (8080 포트)
- TTS API: `venv\Scripts\python.exe tts_api.py` (8090 포트)
- Flutter: `flutter run --dart-define=TTS_TOKEN=vs_xxx` (에뮬레이터 Pixel_10)

## 산출물
| 파일 | 설명 |
|------|------|
| `bible-tts/tts_api.py` | QT 전용 엔드포인트 + CORS 추가 |
| `flutter-app/lib/core/config/app_config.dart` | ttsBaseUrl 추가 |
| `flutter-app/lib/features/tts/services/tts_repository.dart` | TTS API 호출 + 캐싱 |
| `flutter-app/lib/features/tts/providers/tts_providers.dart` | Riverpod 상태 관리 |
| `flutter-app/lib/features/tts/widgets/qt_audio_player.dart` | 오디오 플레이어 위젯 (자동 생성 + 바로 재생) |
| `flutter-app/lib/features/mypage/screens/settings_screen.dart` | QT 읽기 목소리 설정 UI |
| `flutter-app/pubspec.yaml` | just_audio, path_provider 추가 |

## 사용법 (QT 화면에서)
```dart
import 'package:qtai_app/features/tts/widgets/qt_audio_player.dart';

// QT 상세 화면 하단에 배치
QtAudioPlayer(
  qtText: "오늘의 QT 본문 전체 텍스트...",
  qtDate: "2026-06-04",
)
```
