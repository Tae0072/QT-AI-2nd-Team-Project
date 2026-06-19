# 2026-06-19 리포트 — 오늘 QT 해설 스타일 + TTS 수정

| 항목 | 내용 |
|---|---|
| 관련 F-ID | F-08(해설), F-17(TTS) |
| 브랜치 | `feature/today-qt-commentary-style-and-tts-fix` → PR `dev` |
| 범위 | Flutter 앱 (서버 변경 없음) |
| 워크플로 | `doc/workspaces/Lead_강태오/workflows/2026-06-19_today-qt-commentary-style-and-tts-fix.md` |

## 결과 요약
1. 절별 해설 카드의 위계를 색으로 분리: **절 본문(굵게·본문색) → 요약(회색) → 해설(파랑)**. 라이트/다크 테마 토큰으로 자동 대응.
2. TTS 읽어주기 버튼이 재생 중 로딩 스피너로 멈춰 정지/토글이 안 되던 버그를 수정.

## 변경 파일
| 파일 | 구분 | 내용 |
|---|---|---|
| `lib/core/theme/app_theme.dart` | 테마 | `AppColors.explanationBlue` 토큰 신설(라이트 #2563EB / 다크 #8AB4F8), ThemeExtension 6곳 반영 |
| `lib/features/study/screens/qt_study_content_screen.dart` | 화면 | 절 본문 색 `appColors.text`+w700, 해설 색 `appColors.explanationBlue`, 요약 회색 유지 |
| `lib/features/tts/widgets/qt_tts_button.dart` | TTS | `await _player.play()` → `unawaited(_player.play())`, `dart:async` import |
| `test/features/study/screens/qt_study_content_screen_test.dart` | 테스트(신규) | 라이트/다크 스타일 회귀 2건 |

## TTS 진단 상세
- 정상 확인: l10n 키, 에셋/의존성, 호출부 인자, 서버 audio 엔드포인트(raw 바이트 응답), 컴파일.
- **근본 원인**: just_audio의 `play()` Future는 재생 종료 시 완료됨 → `await` 하면 `finally`의 `_isGenerating=false`가 재생 끝까지 지연 → 재생 내내 스피너 + 탭이 '생성 취소'로 처리 + 설정 변경 무시.
- **수정**: 재생은 시작만 하고 기다리지 않음(`unawaited`). 재생 즉시 정지(빨강) 아이콘 표시·정상 토글.

## 검증
| 검증 | 결과 |
|---|---|
| `flutter test .../qt_study_content_screen_test.dart` | ✅ +2 All tests passed (라이트·다크) |
| `flutter analyze` (변경 4파일) | ✅ No issues found |
| TTS 재생 실기기 확인 | ⏳ 후속 — 플랫폼 채널 의존이라 위젯 테스트로 자동화 불가, 1줄 수정·정적분석으로 처리 |

## 주의 / 후속
- `explanationBlue`는 Calm Paper 모노크롬 원칙의 예외(유채색)이며 해설 본문 1곳 한정. 추후 다른 곳 사용 시 디자인 검토 필요.
- TTS 실기기 재생 확인(본문 캐시 음성/콜드스타트 포함)은 디바이스에서 후속 점검.
- 문서레포(07/25) F-08·F-17 텍스트엔 영향 없음(스타일·버그수정 범위).
