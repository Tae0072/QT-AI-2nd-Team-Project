# 2026-06-19 오늘 QT — 해설 텍스트 스타일 + TTS(읽어주기) 고장 수정

## 요청 (Lead T)
같은 '오늘 QT' 화면을 건드리는 두 작업을 한 브랜치에서 처리.

1. **QT 해설 텍스트 스타일** — 절 본문은 굵게, 그 아래 요약줄은 회색 유지, 그 아래 해설줄은 파란색. 라이트/다크 모두 잘 보이게 테마 색 사용.
2. **TTS(읽어주기) 고장 점검·수정** — 먼저 무엇이 깨졌는지 진단 결과를 알리고 고친다.

## 관련 F-ID
- **F-08** 본문 요약·해설·용어 풀이 (해설 화면 = `qt_study_content_screen`)
- **F-17** TTS — 본문과 해설 음성 재생

## 범위 / 브랜치
- 브랜치 `feature/today-qt-commentary-style-and-tts-fix` (origin/dev 기준 worktree).
- 변경: Flutter 앱만(서버 변경 없음). 파일 6개(소스 3 + 테스트 1 + 문서 2).
- 메인 작업 트리가 다른 브랜치/인덱스 작업 중이라, 격리된 worktree에서 이 작업만 커밋.

## 작업 1 — 해설 스타일 (F-08)
대상: `lib/features/study/screens/qt_study_content_screen.dart` 의 `_ExplanationItem`(절별 해설 카드).
한 카드 = [절 번호+절 본문 Row] → [요약(`explanation.summary`)] → [해설(`explanation.explanation`)].

- **절 본문**: 원래 `bodyMedium`(기본색=회색 text2)+w700이라 굵어도 본문이 회색으로 묻혔다 → 색을 `appColors.text`(본문 강조색)로 올리고 w700 유지.
- **요약**: 원래 `titleSmall`(색=text2 회색). 요청대로 **회색 유지**(변경 없음).
- **해설**: 원래 `bodyMedium`(회색) → 색을 **`appColors.explanationBlue`(파랑)** 로.

### 테마 색 결정
앱은 의도적 모노크롬(Calm Paper) 디자인이라 유채색 토큰이 빨강 `accentDot`뿐이고 **파랑 토큰이 없었다.** colorScheme.primary도 파랑이 아니라 잉크색이다. 화면 규칙상 "색 하드코딩 금지, AppColors 토큰 사용"이므로,
`AppColors`에 **`explanationBlue` 토큰을 신설**(라이트 `#2563EB`, 다크 `#8AB4F8`)하고 해설에 사용. ThemeExtension의 6곳(라이트/다크 인스턴스·생성자·필드·copyWith·lerp) 모두 반영해 다크 전환 시 자동 교체된다.
> 디자인 노트: 모노크롬 원칙의 예외(유채색)이며 Lead 요청(가독성) 근거로 도입. 사용처는 해설 본문 1곳으로 한정.

## 작업 2 — TTS 진단·수정 (F-17)
### 진단 (정적 분석)
정상 확인한 것: l10n 키 9종 존재, `tts_voice.png` 에셋/`just_audio` 의존성 존재, 호출부(`today_qt_screen`)가 `qtPassageId/qtText` 정상 전달, 서버 `/api/v1/qt/passages/{id}/audio`가 raw 바이트로 응답, 컴파일 오류 없음.

**근본 원인 (버튼 상태머신 버그):** `qt_tts_button.dart`의 `_prepareAudio`에서
```dart
if (autoPlay) await _player.play();
```
just_audio의 `play()`가 반환하는 Future는 **재생이 끝날 때** 완료된다. 이를 `await`하면 `finally`의 `_isGenerating = false`가 음성이 다 끝날 때까지 실행되지 않는다. 그 결과 **재생 내내 버튼이 로딩 스피너로 멈춰** 보이고, 그동안 탭하면 재생/정지가 아니라 '생성 취소(_cancelGeneration)'로 처리되며, 목소리/범위 설정 변경도 무시된다(재생 중 `_isGenerating==true`라 `_prepareAudio` 조기 return). 즉 재생/정지 토글이 사실상 망가져 "TTS가 안 되는 것처럼" 보인다.

### 수정
```dart
if (autoPlay) unawaited(_player.play());   // 시작만 하고 기다리지 않음
```
`dart:async`의 `unawaited` import 추가. 이제 재생 시작 즉시 `_isGenerating=false`가 되어 정지(빨강) 아이콘이 뜨고, 탭하면 정상적으로 일시정지+되감기 된다.

## 검증
- `flutter test test/features/study/screens/qt_study_content_screen_test.dart` → **+2 All tests passed** (라이트/다크 스타일 회귀).
- `flutter analyze`(변경 4파일) → **No issues found**.
- TTS 수정은 just_audio 플랫폼 채널 의존이라 `flutter test`로 재생 자동화가 불가 → 정적 분석 + 근본 원인 기반 1줄 수정으로 처리(실기기 재생 확인은 후속).

## Git/PR
- 커밋(Conventional Commits):
  - `style(study): 절별 해설 본문 강조·해설 파랑 + explanationBlue 테마 토큰 [F-08]`
  - `fix(tts): 재생 시 play() await 제거로 로딩 스피너 멈춤·정지불능 해결 [F-17]`
- PR 대상 `dev`. 본문에 F-08/F-17 + 본 워크플로/리포트 링크.
