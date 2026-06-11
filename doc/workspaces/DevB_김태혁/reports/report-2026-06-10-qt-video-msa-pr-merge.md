# Report — 2026-06-10 qt-video-msa-pr-merge

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김태혁 |
| 브랜치 | `feature/study-qt-video-clips` |
| 작업 패널 | QT 영상 클립 MSA 본 프로젝트 반영 및 PR merge |
| PR | https://github.com/Tae0072/QT-AI-2nd-Team-Project/pull/456 |
| 최종 상태 | MERGED |

## 변경 내용

- 기존 simulator 흐름과 분리된 `qt-video` 도메인을 본 프로젝트 MSA 구조에 맞춰 정리했다.
- DB 마이그레이션을 세 경로에 반영했다.
  - legacy root: `qtai-server/src/main/resources/db/migration/V30__create_qt_video_clips.sql`
  - service-bible: `qtai-server/service-bible/src/main/resources/db/migration/V30__create_qt_video_clips.sql`
  - admin-server: `qtai-server/admin-server/src/main/resources/db/migration/V32__create_qt_video_clips.sql`
- 사용자 조회 API를 `GET /api/v1/qt/{qtPassageId}/video`로 분리했다.
- Today QT 응답 enrich를 제거해 `qt -> qtvideo` 양방향 의존과 Today QT 응답 지연 우려를 줄였다.
- 사용자 노출 상태를 정리했다.
  - `APPROVED -> READY`
  - `HIDDEN -> DISABLED`
  - `FAILED -> FAILED`
  - 후보 없음 또는 `PENDING`만 존재 -> `MISSING`
  - 우선순위: `APPROVED > HIDDEN > FAILED`
- OpenAPI에서 simulator clip enum은 기존 `APPROVED`만 유지하고, QT video enum만 `PENDING/APPROVED/FAILED/HIDDEN`으로 확장했다.
- Controller MockMvc 테스트를 추가하고, 타 도메인 internal import를 제거했다.
- `QtVideoUserStatusResolverTest`를 추가해 상태 매핑과 우선순위를 직접 검증했다.
- Flutter Today QT 하단 `QT 영상` 섹션을 정리했다.
  - `QtVideoSection`
  - `QtVideoPlayer`
  - `QtVideoCache`
  - 1일 로컬 캐시
  - 배속/전체화면/컨트롤 자동 숨김
- Flutter 위젯/캐시 테스트를 추가했다.
  - non-ready 상태에서 플레이어 미노출
  - READY + URL 상태에서 플레이어 노출
  - cache key sanitizing
- PR 본문과 DevB Workflow/Report를 최신 리뷰 대응 내용으로 갱신했다.
- PowerShell 인코딩 문제로 깨진 PR 본문을 UTF-8 no BOM 파일 방식으로 복구했다.

## 자동 리뷰 대응 결과

- 1차 반려 대응
  - Controller MockMvc 통합 테스트 추가
  - `FAILED`/`DISABLED` 상태 실제 도달 가능하도록 구현
  - F-01/F-02/F-12 근거와 Lead 승인 기준 문서화
- 2차 반려 대응
  - Flyway SQL을 PR diff에 포함
  - Flutter 위젯/캐시 테스트 추가
  - OpenAPI simulator enum 변경 원복
  - 테스트의 타 도메인 internal import 제거
- 3차 보완
  - resolver 전용 단위 테스트 추가
  - cache 서비스 분리
  - PR 댓글로 migration SQL 포함 여부와 검증 명령 재명시
- 최종 Claude 자동 리뷰 결과: APPROVE

## 검증 결과

- 로컬 검증
  - `./gradlew.bat :service-bible:test :admin-server:test`: PASS
  - `flutter analyze`: PASS
  - `flutter test test/features/bible/widgets/qt_video_player_test.dart`: PASS
  - `git diff --check`: PASS
- GitHub Actions
  - `qtai-server Build & Test`: SUCCESS
  - `Flutter Analyze & Test`: SUCCESS
  - `OpenAPI Spectral Lint`: SUCCESS
  - `Requirements Guard (v3.1)`: SUCCESS
  - `Gitleaks Secret Scan`: SUCCESS
  - `Docker Compose Config Validation`: SUCCESS
  - `ci-all`: SUCCESS
- PR 상태
  - 최종 커밋: `cba5f19 test(qt-video): cover resolver and split cache service`
  - 상태: MERGED

## 확인한 금지선

- AI 자유 챗봇/SSE/RAG 없음
- Kafka/Kubernetes/Helm 신규 도입 없음
- 사용자 요청 시 AI 생성 또는 영상 컷팅 없음
- 금지 번역본/찬양 가사/음원 저장 없음
- 직접 YouTube URL 저장 없음
- 기존 simulator API/테이블 삭제 없음
- 성경 본문 텍스트 신규 저장 없음
- 공개 관리자 import API 없음

## 남은 리스크

- 운영 클라우드 저장소/CDN 확정 필요
- 운영 MySQL migration/import는 실제 배포 환경에서 별도 수행 필요
- 관리자 import API 또는 내부 import tool은 후속 PR에서 결정
- 00:05 영상 컷팅 배치는 현재 AI 시딩과 이름/책임이 충돌하지 않도록 별도 기능명과 문서 승인 후 진행
- `QtVideoPlayer` 파일은 아직 큰 편이라 `_QtVideoFullscreen`, `_VideoControls`, `_VideoFrame` 분리는 후속 개선 가능
- 운영 import 단계에서 `videoUrl` CDN 도메인 화이트리스트 검증 정책 필요

## 회고

오늘 작업은 기능 구현보다 PR을 실제 팀 기준에 맞게 통과시키는 과정이 핵심이었다. 영상 URL을 재생하는 기능 자체보다 도메인 경계, 상태 정책, Flyway, OpenAPI, 테스트, F-ID 근거, PR 본문 인코딩까지 모두 맞아야 merge가 가능했다. QT 영상 기능은 이제 `bible-engine`의 산출물을 앱에서 쓰기 위한 실서비스 구조로 한 단계 들어갔다.
