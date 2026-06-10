# Workflow — 2026-06-10 qt-video-msa-pr-merge

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/study-qt-video-clips` |
| 작업 패널 | QT 영상 클립 MSA 본 프로젝트 반영 및 PR merge |
| 기능 ID | F-01 / F-02 / F-12 |
| 기준 문서 | `CLAUDE.md`, 요구사항 정의서 F-01/F-02/F-12, DevB workflow/report |

## 작업 목표

- `bible-engine`에서 만든 고린도전서 영상 흐름을 MSA 재설계된 본 프로젝트에 맞게 반영한다.
- 기존 simulator API/테이블을 삭제하거나 덮어쓰지 않고, QT 영상 전용 `qt-video` 도메인으로 분리한다.
- 사용자 앱은 Today QT 하단에서 사전 승인된 QT 영상 클립만 조회/재생하도록 한다.
- 자동 리뷰 반려 사항을 모두 해결하고 PR #456을 merge 가능한 상태로 만든다.

## 수정 예정 경로

- `qtai-server/service-bible/src/main/java/com/qtai/domain/qtvideo/**`
- `qtai-server/service-bible/src/test/java/com/qtai/domain/qtvideo/**`
- `qtai-server/service-bible/src/test/java/com/qtai/bible/QtVideoControllerTest.java`
- `qtai-server/service-bible/src/main/resources/db/migration/V30__create_qt_video_clips.sql`
- `qtai-server/admin-server/src/main/resources/db/migration/V32__create_qt_video_clips.sql`
- `qtai-server/src/main/resources/db/migration/V30__create_qt_video_clips.sql`
- `qtai-server/apis/api-v1/openapi.yaml`
- `flutter-app/lib/features/bible/widgets/qt_video_player.dart`
- `flutter-app/lib/features/bible/services/qt_video_cache.dart`
- `flutter-app/test/features/bible/widgets/qt_video_player_test.dart`
- `doc/workspaces/DevB_김태혁/workflows/2026-06-10_qt-video-clips-msa.md`
- `doc/workspaces/DevB_김태혁/reports/2026-06-10_qt-video-clips-msa_report.md`

## 검증 계획

- `./gradlew.bat :service-bible:test :admin-server:test`
- `flutter analyze`
- `flutter test test/features/bible/widgets/qt_video_player_test.dart`
- `git diff --check`
- GitHub Actions 확인
  - `qtai-server Build & Test`
  - `Flutter Analyze & Test`
  - `OpenAPI Spectral Lint`
  - `Requirements Guard (v3.1)`
  - `Gitleaks Secret Scan`
- Claude 자동 리뷰 결과 확인

## 리뷰 대응 계획

- Controller MockMvc 통합 테스트 부재 대응
- `FAILED` / `DISABLED` 상태 도달 불가 대응
- Flyway migration SQL 누락 대응
- 신규 도메인 F-ID/Lead 근거 명시
- Flutter 위젯/캐시 테스트 추가
- OpenAPI simulator enum 변경 원복 및 QT video enum 분리
- 테스트의 타 도메인 internal import 제거
- resolver 단위 테스트 추가
- Flutter cache 책임 분리

## 막힌 점

- PowerShell 파이프 인코딩으로 PR 본문 한글이 깨질 수 있어, PR 본문 수정은 UTF-8 파일 방식으로 처리한다.
- 운영 클라우드 저장소/CDN, 관리자 import API, 00:05 영상 컷팅 배치는 이번 PR 범위 밖으로 둔다.
