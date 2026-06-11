# 2026-06-11 QT 영상 클립 자동 준비 버그 수정 워크플로우

## 목표

QT 본문과 절별 영상 timecode가 DB에 존재해도 `qt_video_clips` row가 자동 생성되지 않아 앱에서 QT 영상이 `MISSING`으로 보이는 문제를 수정한다.

## 배경

기존 QT 영상 구조는 다음 데이터를 전제로 한다.

- `source_videos`: 성경 권 단위 원본 영상 URL
- `bible_verse_video_segments`: 절별 시작/종료 timecode
- `qt_video_clips`: QT 본문에 연결된 사용자 노출 클립

문제는 `source_videos`와 `bible_verse_video_segments`가 준비되어 있어도, 매일 들어오는 QT 본문에 맞춰 `qt_video_clips`가 자동으로 만들어지지 않는 점이었다. 이 때문에 프론트는 `/api/v1/qt/{qtPassageId}/video` 응답에서 `MISSING`을 받고 QT 영상 섹션을 숨기게 된다.

## 범위

- 새 DB 테이블, 새 API, OpenAPI 변경은 하지 않는다.
- 기존 QT 영상 조회 API 계약은 유지한다.
- `qt_passage_verses` 저장 완료 이후 해당 `qtPassageId` 기준으로 QT 영상 클립을 자동 준비한다.
- 서버 시작 시 오늘 QT에 대해 누락된 클립을 한 번 보정한다.
- 운영자가 `HIDDEN`으로 비활성화한 QT 영상은 자동으로 다시 `APPROVED`로 살리지 않는다.

## 설계

1. QT 도메인에서 verse mapping 저장 완료 후 `QtPassageVerseMappingsChangedEvent`를 발행한다.
2. qtvideo 도메인은 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 이벤트를 수신한다.
3. `QtVideoClipPreparationService`가 QT 본문 verse ids를 조회한다.
4. 같은 active source video 안에 QT verse ids 전체 timecode가 존재하는지 확인한다.
5. 가장 이른 `start_time_sec`, 가장 늦은 `end_time_sec`로 `SINGLE_CUT` 클립을 계산한다.
6. active clip이 없으면 `APPROVED`로 생성하고, 이미 있으면 같은 row를 갱신한다.

## 예외 처리

- QT 본문이 비공개면 skip
- verse mapping이 없으면 skip
- 일부 절 timecode가 없으면 skip
- `start_time_sec >= end_time_sec`면 skip
- 기존 `HIDDEN` 클립이 있으면 운영자 비활성화로 보고 skip

## 검증 계획

- `QtVideoClipPreparationServiceTest`
  - 전체 verse timecode가 있으면 `APPROVED` clip 생성
  - 기존 active clip이 있으면 중복 row 없이 갱신
  - 일부 verse timecode가 빠지면 생성하지 않음
  - `HIDDEN` clip은 자동으로 다시 살리지 않음
  - 서버 시작 시 오늘 QT clip 준비
  - 오늘이 아닌 날짜의 QT도 `qtPassageId` 기준으로 준비 가능
- `./gradlew.bat :service-bible:test`
- `git diff --check origin/dev...HEAD`

## 기준 문서

- `doc/workspaces/DevB_김태혁/workflows/2026-06-10_qt-video-clips-msa.md`
- `doc/workspaces/DevB_김태혁/reports/2026-06-10_qt-video-clips-msa_report.md`
- `doc/admin-server-sync-rules.md`
