# 2026-06-10 QT 영상 클립 MSA 적용 리포트

## 요약

QT 영상 클립 기능을 기존 simulator 흐름과 분리해 `qt-video` 도메인으로 구현했다.

- 백엔드 담당 서비스: `service-bible`
- 사용자 API: `GET /api/v1/qt/{qtPassageId}/video`
- Flutter Today QT 화면 하단에 `QT 영상` 섹션 추가
- 기존 simulator API/테이블은 삭제하거나 변경하지 않음
- Today QT 응답의 영상 enrich는 제거해 `qt -> qtvideo` 양방향 의존과 추가 DB 조회를 피함

## 리뷰 반영

1. Controller MockMvc 통합 테스트 추가
   - `QtVideoControllerTest` 추가
   - 200 envelope, MISSING payload, 400, 401, 404 경로 검증
   - `ControllerSecurityIntegrationTest`에 `/api/v1/qt/1/video` 미인증 401 검증 추가

2. `FAILED` / `DISABLED` 상태 도달 가능하도록 수정
   - `APPROVED -> READY`
   - `FAILED -> FAILED`
   - `HIDDEN -> DISABLED`
   - 후보 없음 또는 `PENDING`만 존재 -> `MISSING`
   - 상태 매핑 단위 테스트 추가

3. F-ID 근거 명시
   - F-01: Today QT 화면에서 QT 영상 섹션 제공
   - F-02: 사용자 요청 시 AI 생성/영상 컷팅 금지 정책 준수
   - F-12: 사전 준비된 클립의 상태 기반 재생 진입점 제공

4. 양방향 모듈 의존 완화
   - 기존 설계의 `TodayQtResponse.videoStatus` enrich를 제거
   - `QtService`는 더 이상 `qtvideo` 도메인에 의존하지 않음
   - 영상 조회는 Flutter `QtVideoSection`이 `/api/v1/qt/{qtPassageId}/video`를 별도 호출

## 백엔드 변경

- DB 마이그레이션 추가
  - `source_videos`
  - `bible_verse_video_segments`
  - `qt_video_clips`
- `qtvideo` 도메인 추가
  - `api`: `GetQtVideoUseCase`, `QtVideoClipResponse`
  - `internal`: `SourceVideo`, `BibleVerseVideoSegment`, `QtVideoClip`, `QtVideoService`, repository, 상태 매핑 resolver
  - `web`: `QtVideoController`
- OpenAPI에 QT 영상 endpoint와 상태 enum 반영

## Flutter 변경

- `video_player` 추가
- `QtVideoClip` 모델과 `qtVideoClipProvider` 추가
- `QtVideoSection` / `QtVideoPlayer` 추가
- 플레이어 기능
  - 재생/일시정지
  - 진행바
  - 배속
  - 전체화면
  - 컨트롤 자동 숨김
  - 1일 로컬 캐시

## 운영 메모

1. 운영 DB에 `V30__create_qt_video_clips.sql` 적용
2. 고린도전서 원본 영상 row를 `source_videos`에 등록
3. 절별 timecode를 `bible_verse_video_segments`에 등록
4. 해당 QT 본문 클립을 `qt_video_clips`에 등록
   - 노출 가능: `APPROVED`, `active_unique_key='ACTIVE'`
   - 실패 표시: `FAILED`
   - 운영 비활성: `HIDDEN`
5. 1차 import 템플릿: `scripts/qt-video-1co-import-template.sql`

## 검증

- `./gradlew.bat :service-bible:test` 통과
- `flutter analyze` 통과
- `git diff --check` 통과
  - 공백 오류 없음
  - Windows 작업환경의 LF -> CRLF 안내만 출력됨

## 남은 작업

- 운영 클라우드 저장소/CDN URL 확정
- 실제 운영 MySQL에 migration/import 적용
- 필요 시 관리자 import API 또는 내부 import tool 추가
- 00:05 자동 컷팅 배치는 별도 기능명과 별도 문서 승인 후 진행
