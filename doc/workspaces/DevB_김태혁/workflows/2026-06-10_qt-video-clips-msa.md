# 2026-06-10 QT 영상 클립 MSA 적용 워크플로우

## 목표

기존 시뮬레이터 흐름을 삭제하거나 재사용하지 않고, QT 본문에 연결된 승인 영상 클립을 별도 `qt-video` 도메인으로 조회/재생한다.

## F-ID 근거

- F-01 성경/QT 본문 화면: Today QT 화면 하단에서 QT 영상 섹션을 제공한다.
- F-02 AI 콘텐츠 사전 생성: 사용자 요청 시점에 AI 생성 또는 영상 컷팅을 수행하지 않는다.
- F-12 시뮬레이터 보기: 기존 “사전 생성 클립 상태/재생 진입점” 요구를 영상 클립 재생 방식으로 분리 구현한다.

이번 PR은 신규 제품 기능 신설이 아니라 F-12의 클립 재생 진입점을 `qt-video` 도메인으로 구체화한 구현이다. 따라서 새 F-ID를 신설하지 않고 F-01/F-02/F-12 하위 구현으로 정리한다. Lead가 별도 제품 기능으로 분리 판단하는 경우에는 본 PR 범위가 아니라 후속 PR에서 신규 F-ID를 신설한다.

## Lead 승인 정리

- 신규 F-ID 신설: 없음
- 현재 PR 판단: F-12 하위 구현
- Lead 추가 승인 필요 조건: “QT 영상”을 기존 사전 생성 클립 재생과 다른 별도 제품 기능으로 분리하기로 결정하는 경우
- 현재 PR의 merge 판단 근거: 기존 F-ID 범위 안에서 도메인명을 `qt-video`로 분리해 구현하며, 기존 simulator API/테이블을 변경하지 않음

## 범위

- 기존 simulator API와 `simulator_clips` 동작은 변경하지 않는다.
- `source_videos`, `bible_verse_video_segments`, `qt_video_clips` 테이블을 추가한다.
- Flyway 마이그레이션을 legacy root, admin-server, service-bible resource 경로에 반영한다.
- 사용자 API `GET /api/v1/qt/{qtPassageId}/video`를 추가한다.
- Today QT 응답에는 영상 상태를 enrich하지 않는다.
  - 이유: `qt -> qtvideo` 양방향 의존과 Today QT 응답 지연을 피하기 위해서다.
  - Flutter는 QT 본문 ID가 있을 때 QT 영상 섹션에서 `/video` API를 별도 호출한다.
- Flutter Today QT 화면 하단에 QT 영상 플레이어를 추가한다.
- Flutter 캐시 로직은 위젯 파일에서 분리해 `services/qt_video_cache.dart`에서 담당한다.

## 비범위

- AI 영상 생성 없음.
- 00:05 자동 영상 컷팅 배치 없음.
- 공개 업로드 API 또는 관리자 import API 없음.
- 운영 클라우드 저장소/CDN 연동 없음.

## 상태 정책

- `qt_video_clips.status=APPROVED` -> 사용자 상태 `READY`
- `qt_video_clips.status=HIDDEN` -> 사용자 상태 `DISABLED`
- `qt_video_clips.status=FAILED` -> 사용자 상태 `FAILED`
- 후보 클립 없음 또는 `PENDING`만 존재 -> 사용자 상태 `MISSING`

`FAILED`와 `DISABLED`는 URL 없는 status-only 응답으로 내려가며, Flutter는 `READY`와 유효한 `videoUrl`일 때만 플레이어를 렌더링한다.

상태 우선순위는 `APPROVED > HIDDEN > FAILED`다. 운영자가 숨김 처리한 `HIDDEN`은 자동 실패 상태보다 강한 의도로 본다.

## 검증 계획

- `./gradlew.bat :service-bible:test`
- `./gradlew.bat :admin-server:test`
- `flutter analyze`
- `flutter test test/features/bible/widgets/qt_video_player_test.dart`
- `git diff --check`
- PR 본문에 MockMvc 테스트, resolver 단위 테스트, Flutter 위젯/캐시 테스트, F-ID 근거, Today QT enrich 제거 결과를 기록한다.
