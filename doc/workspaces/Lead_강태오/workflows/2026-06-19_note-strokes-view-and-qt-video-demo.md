# 워크플로우 — 노트 상세 손그림 표시 + QT영상 시연 데모 + 기본 라이트모드 확인

- 날짜: 2026-06-19
- 작성: Lead 강태오 (with Claude)
- 브랜치: `feature/note-strokes-view-and-qt-video-demo` → PR to `dev`
- F-ID: F-03(노트/손그림), F-12/F-06(QT 시뮬레이터 영상)

## 배경 — 사용자 보고 3건
1. QT영상이 안 나온다(시연용으로 영상만 나오면 됨).
2. 노트에 펜으로 그린 그림이 저장이 안 되는 것처럼 보인다(텍스트만 보임).
3. 기본을 라이트모드로(사용자가 설정 안 하면 무조건 라이트).

## 진단

### (3) 기본 라이트모드 — 이미 dev에 반영됨(코드 변경 불필요)
- `core/theme/theme_providers.dart` `_initial`이 **저장값 없으면 `ThemeMode.light`** 반환(이미 dev에 머지됨).
- 사용자가 빌드한 브랜치(`feature/mypage-os-notifications`)가 dev보다 뒤처져 `ThemeMode.system`이라, 기기가 다크면 다크로 보였던 것.
- 조치: **dev를 머지/빌드하면 해결**. 본 PR에서 테마 코드는 건드리지 않음.

### (2) 펜 그림 — 저장은 됨, '상세(보기) 화면'이 안 그렸음
- 펜 획은 `NoteCanvasStore`(앱 문서폴더 JSON, 키별)로 **로컬 저장**되고, 편집기는 그릴 때마다 저장/재로드한다(정상).
- 그러나 노트를 탭하면 가는 **상세 화면(`note_detail_screen`)이 그림을 전혀 불러오지 않아** 텍스트만 보였다 → "펜 저장 안 됨"으로 오인.

### (1) QT영상 — 오늘 QT에 준비된 시뮬레이터 클립이 없음
- `qtVideoClipProvider`는 `clip.isReady`(서버 status READY + videoUrl)일 때만 영상 섹션을 표시. 준비된 클립이 없으면 섹션이 숨겨진다.

## 한 일

### (2) 노트 상세에 손그림 읽기 전용 렌더 (`note_detail_screen.dart`)
- `_DetailBody`를 `ConsumerWidget`으로 바꿔 편집기와 **동일 키**(`qt:{qtPassageId}`(묵상)·`note:{noteId}`(그 외))로 획을 로드.
- 본문 위에 `NoteDrawingLayer`(읽기 전용: enabled=false)를 겹쳐 그려 저장된 그림을 보여준다. 좌표는 0~1 정규화라 박스 크기에 맞춰 비례 표시.

### (1) QT영상 시연 데모 오버라이드 (`bible/providers/dev_demo_qt_video.dart` 신규 + `bible_providers.dart`)
- **디버그 + 클립이 준비 안 됨**일 때만 샘플 영상(Flutter 공식 데모 `butterfly.mp4`)으로 대체해 항상 재생되게 함.
- 안전장치: ① `kDebugMode` 아니면 컴파일에서 제거(릴리스 무영향) ② 서버가 **이미 READY 클립을 주면 그대로 사용** ③ 별도 파일로 격리 — 운영 전 제거 대상.
- 실제 데모 영상이 있으면 `kDemoQtVideoUrl` 한 줄만 바꾸면 됨.

## 검증
- `flutter analyze lib/features/note lib/features/bible` → **No issues found**.
- `flutter test test/features/note test/features/bible` → **All tests passed (133)**.

## 운영 전 제거(데모)
- `dev_demo_qt_video.dart` 삭제 + `bible_providers.dart`의 import·`withDemoQtVideo` 호출 제거.
- (노트 상세 손그림 렌더는 정식 기능이라 유지.)
