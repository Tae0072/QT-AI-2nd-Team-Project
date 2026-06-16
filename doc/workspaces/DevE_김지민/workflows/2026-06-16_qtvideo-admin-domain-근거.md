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

## 5. 삭제 정책: soft-delete 채택(프로젝트 공통 정책과 통일)

- 삭제는 **soft-delete**로 통일했다(노트·나눔 등 기존 도메인과 동일). 행을 물리 삭제하지 않고 `deleted_at`을 기록하고 `active_unique_key`를 비운다.
  - 엔티티 `softDelete(deletedAt)`: `SourceVideo`/`QtVideoClip`(active_unique_key=null + markDeletedAt), `BibleVerseVideoSegment`(markDeletedAt).
  - 원본 영상 soft-delete 시 그 원본의 클립·구간도 동반 soft-delete(cascade).
  - 목록 조회는 `deleted_at IS NULL` 필터로 삭제분을 제외한다. 활성 선택(`active_unique_key='ACTIVE'`) 경로는 키가 비워져 자동 제외되므로 유저앱(service-bible) 조회는 변경 없이 삭제분이 노출되지 않는다.
  - 감사 로그는 삭제 직전 상태를 before-state로 남긴다(동반 삭제된 클립·구간 수 포함).
- `replaceSegments`만 예외적으로 기존 구간을 **물리 삭제** 후 재삽입한다. `(bible_verse_id, source_video_id)` 유니크 제약이 `deleted_at`을 포함하지 않아, soft-delete로 남겨두면 동일 절 재삽입 시 충돌하기 때문이다.

## 6. Lead 확인 요청 항목

- [ ] AD-20 관리자 도메인(admin-server `qtvideo`)을 정식 운영 도메인으로 승인.
- [ ] (참고) 삭제 정책은 프로젝트 관례에 맞춰 soft-delete로 확정함 — 추가 이견 없으면 그대로 진행.
