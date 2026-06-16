# QT 영상 관리(AD-20) admin 도메인 신설 근거 — qtvideo

- 작성일: 2026-06-16
- 작성자: 김지민(DevE)
- 관련 PR: #686 `feat(admin): QT 영상 관리 삭제·구간 운영 보강`
- 관련 F-ID: F-06(관리자 운영), F-12(시뮬레이터/영상)
- 목적: PR 리뷰에서 제기된 "신규 `qtvideo` 도메인 신설의 문서·Lead 승인 근거" 항목에 대한 근거 정리

## 1. `qtvideo` 도메인은 신규 신설이 아니라 기존 도메인의 admin 측 확장이다

- 유저용 `qtvideo` 도메인(클립 준비 엔진·유저앱 영상 조회·READY 판정)은 **service-bible에 이미 존재**한다.
  - 근거 커밋: `680395d5 fix(qt-video): QT 영상 READY 판정 및 캐시 보정 (#569)` 등 (작성자: xogurrh012, 2026-06-13)
  - 경로: `qtai-server/service-bible/.../domain/qtvideo/**`
- 본 PR(AD-20)은 그 위에 **관리자 운영 기능**(원본 영상 등록/수정/삭제, 절별 구간 저장, 클립 생성/상태변경/삭제)을 admin-server에 추가한 것이다.
  - 경로: `qtai-server/admin-server/.../domain/qtvideo/**`

## 2. CLAUDE.md 규칙상 admin-server에 두는 것이 맞다

- `CLAUDE.md §3`: 도메인 목록에 운영 기능을 담는 `admin` 경계가 정의돼 있고, 관리자 고유 컨트롤러·배치는 admin-server에서 직접 구현한다(admin-server-sync-rules).
- `CLAUDE.md §1` admin-server 동기화 규칙: "admin 고유 기능(admin 컨트롤러·관리자 배치)만 admin-server에서 직접 수정한다." → AD-20 관리자 API는 admin 고유 기능이므로 admin-server 소유가 맞다.
- 스키마(Flyway)는 admin-server 단독 소유 규칙에 따라, qtvideo 3개 테이블은 admin-server 마이그레이션 `V32__create_qt_video_clips.sql`에 정의돼 있다(아래 §3).

## 3. 스키마(Flyway) 누락 아님 — V32가 3개 테이블을 모두 생성한다

`qtai-server/admin-server/src/main/resources/db/migration/V32__create_qt_video_clips.sql` 가 다음 3개 테이블을 모두 생성한다.

- `source_videos`
- `bible_verse_video_segments`
- `qt_video_clips`

이 V32 마이그레이션은 **이미 dev/master에 머지돼 있고**(service-bible QT 영상 기능과 함께 들어옴), 본 PR diff에는 포함되지 않는다. 따라서 "신규 3 테이블 마이그레이션 누락"은 PR diff만 본 데서 비롯된 오탐이다. admin-server 엔티티는 `ddl-auto: validate`로 이 스키마와 정합성을 검증한다(테스트/기동 통과 확인).

## 4. 도메인 경계(ArchUnit) 준수

- admin `qtvideo` 코드는 타 도메인의 `internal`을 직접 import하지 않는다.
- 성경 절 조회·QT 본문 컨텍스트는 `bible.api`/`qt.api`의 UseCase·DTO를 통해서만 접근한다(`GetBibleVerseUseCase`, `ListBibleBooksUseCase`, `GetQtPassageContentContextUseCase`).

## 5. Lead 확인 요청 항목

- [ ] AD-20 관리자 도메인(admin-server `qtvideo`)을 정식 운영 도메인으로 승인.
- [ ] 삭제 정책: 현재 **하드 삭제 + cascade**(원본 삭제 시 클립·구간 동반 삭제). soft-delete(`deleted_at`) 전환 필요 여부 Lead 판단 요청. 현 결정 근거는 "운영자가 잘못 등록한 원본/클립을 깔끔히 제거" 목적.
