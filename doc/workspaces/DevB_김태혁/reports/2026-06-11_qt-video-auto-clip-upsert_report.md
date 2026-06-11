# 2026-06-11 QT 영상 클립 자동 준비 버그 수정 리포트

## 요약

QT 영상 데이터가 준비되어 있어도 `qt_video_clips`가 자동 생성되지 않아 앱에서 QT 영상이 노출되지 않는 문제를 수정했다.

이번 변경은 새 기능 추가가 아니라 기존 QT 영상 클립 기능의 운영 결함 보정이다. DB 스키마, API 경로, OpenAPI enum은 변경하지 않았다.

## 변경 내용

1. QT verse mapping 저장 완료 이벤트 추가
   - `QtPassageVerseMappingsChangedEvent` 추가
   - `QtTodayPassageImportService`에서 verse mapping 저장 후 이벤트 발행

2. QT 영상 클립 자동 준비 서비스 추가
   - `QtVideoClipPreparationService` 추가
   - QT 본문 verse ids와 `bible_verse_video_segments`를 기준으로 source video, start/end timecode 계산
   - active `qt_video_clips` row를 생성 또는 갱신

3. 이벤트 리스너 추가
   - 서버 시작 시 오늘 QT 누락 클립 보정
   - QT verse mapping import 트랜잭션 커밋 이후 해당 `qtPassageId` 클립 준비

4. Repository/Entity 보강
   - active clip 조회 메서드 추가
   - `HIDDEN` 상태 존재 여부 확인 메서드 추가
   - 기존 active clip을 `APPROVED SINGLE_CUT`으로 갱신하는 도메인 메서드 추가

5. 테스트 추가
   - 생성, 갱신, 누락 timecode, 수동 HIDDEN, 오늘 QT 보정, 비오늘 날짜 passage 준비 검증

## 운영 메모

- source video와 verse timecode가 없으면 자동 생성하지 않고 `MISSING` 상태가 유지된다.
- `HIDDEN`은 운영자가 의도적으로 비활성화한 상태로 보고 자동 복구하지 않는다.
- 서버 시작 시 전체 과거 QT를 백필하지 않는다. 시작 시 보정 대상은 오늘 QT만이다.
- 과거 QT 전체 백필이 필요하면 별도 배치 또는 운영 import 작업으로 분리한다.

## admin-server 복사본 반영 여부

해당 없음.

이번 변경은 `service-bible`의 QT 영상 자동 준비 런타임 로직이며, admin-server 관리자 복사본이나 Flyway 마이그레이션을 변경하지 않는다.

## 검증

- `.\gradlew.bat :service-bible:test --tests "com.qtai.domain.qtvideo.internal.QtVideoClipPreparationServiceTest"` 통과
- `.\gradlew.bat :service-bible:test` 통과
- `git diff --check origin/dev...HEAD` 통과

## 남은 작업

- 운영 DB에 `source_videos`, `bible_verse_video_segments` 데이터가 준비되어 있어야 한다.
- 운영 CDN 또는 파일 저장소 URL이 앱에서 접근 가능한 URL이어야 한다.
- 과거 QT 전체 백필은 이번 PR 범위가 아니며 필요 시 별도 작업으로 진행한다.
