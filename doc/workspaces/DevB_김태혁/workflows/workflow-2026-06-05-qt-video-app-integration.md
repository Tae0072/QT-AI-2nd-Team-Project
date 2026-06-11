# Workflow — 2026-06-05 qt-video-app-integration

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | QT-AI-2nd-Team-Project `dev` |
| 작업 패널 | QT 본문 기반 영상 앱 연동 |
| 기능 ID | qtai-server / flutter-app / QT 영상 |
| 기준 문서 | workflow-2026-06-05-video-batch-render, report-2026-06-05-video-batch-render |

## 작업 목표
- 성경 영상 렌더 결과물을 오늘 QT 화면 맨 아래에서 재생할 수 있게 연결한다.
- 시뮬레이터가 아니라 별도 `QT 영상` 기능으로 분리한다.
- 백엔드는 QT 본문 ID 기준으로 승인된 영상 URL과 재생 구간을 내려준다.
- 프론트는 `READY` 상태일 때만 영상 플레이어를 표시한다.

## 수정 예정 경로
- `QT-AI-2nd-Team-Project/qtai-server/src/main/resources/db/migration/V24__create_qt_video_assets.sql`
- `QT-AI-2nd-Team-Project/qtai-server/src/main/java/com/qtai/domain/qt/**`
- `QT-AI-2nd-Team-Project/qtai-server/src/test/java/com/qtai/domain/qt/**`
- `QT-AI-2nd-Team-Project/flutter-app/lib/features/bible/**`
- `QT-AI-2nd-Team-Project/flutter-app/lib/core/network/api_client.dart`
- `QT-AI-2nd-Team-Project/flutter-app/android/app/src/debug/AndroidManifest.xml`
- `QT-AI-2nd-Team-Project/flutter-app/android/app/src/profile/AndroidManifest.xml`
- `bible-engine/public/videos/corinthians11_v14_10sec_exact.mp4`

## 검증 계획
- `qt_passage_video_clips`에 승인된 QT 영상 클립이 저장되는지 확인한다.
- `GET /api/v1/qt/{qtPassageId}/video`가 `READY` 응답을 내려주는지 확인한다.
- 로컬 영상 서버가 mp4 Range 요청에 `206 Partial Content`로 응답하는지 확인한다.
- Flutter 오늘 QT 화면 맨 아래에 `QT 영상` 섹션이 표시되는지 확인한다.
- Android 에뮬레이터에서 영상 프레임, 재생 버튼, 진행바가 렌더링되는지 확인한다.
- `./gradlew.bat clean test`, `flutter analyze`, `flutter test`를 실행한다.

## 막힌 점
- 로컬 H2 메모리 DB는 재시작 시 seed가 사라진다.
- 1차 테스트 파일은 고린도전서 전체 1권 영상이 아니라 `11:1-14` 샘플 영상이다.
- 운영 적용 전에는 66권 `source_videos` 등록과 QT별 clip 생성/import 흐름이 필요하다.
