# 2026-06-14 전체 조회 속도 개선 — 목록 인덱스 추가

## 진단(가장 느린 곳)
주요 사용자 목록 조회 쿼리를 훑어 N+1·인덱스·페이징·블롭 로딩을 점검했다.
- **나눔 피드 `GET /api/v1/sharing-posts`(가장 느림)**: `WHERE status='PUBLISHED' ORDER BY created_at DESC`. 기존 인덱스는 `idx_sharing_posts_member(member_id)`뿐 → 발행글 필터+정렬이 **풀 테이블 스캔**. (N+1은 이미 좋아요/댓글/프로필 배치로 해결돼 있음.)
- **노트 목록 `GET /api/v1/notes`**: `WHERE member_id=? [AND category/status] ORDER BY updated_at DESC`. member 인덱스는 있으나 `updated_at` 정렬 커버 인덱스가 없어 '전체' 조회 시 filesort.
- 음악 목록: `MusicTrackSummary` projection으로 blob 미로딩 + `idx_music_tracks_enabled_sort` 매칭 → 양호.
- 묵상 달력: 월 범위에 `idx_notes_member_saved_at` 사용 → 양호.
- 정렬 매핑 확인: 나눔 `publishedAt`은 `SharingPostService`에서 `createdAt`으로 매핑(별도 published_at 컬럼 없음), 노트는 `updatedAt`.

## 조치 (V39 마이그레이션, admin-server 단독 소유)
- `idx_sharing_posts_status_created (status, created_at)` — 발행 피드 필터+정렬을 인덱스 범위 스캔으로.
- `idx_notes_member_updated (member_id, updated_at)` — 노트 '전체' 조회 정렬을 인덱스로.
- MySQL 8.0/H2 호환 위해 부분(WHERE)·DESC 키워드 없는 일반 복합 인덱스. DESC 정렬은 옵티마이저 역방향 스캔으로 처리.

## 한계
- 검색어(q) `LIKE '%..%'`는 선행 와일드카드라 인덱스를 못 타는 본질적 제약(별도 풀텍스트 과제). 이번 개선은 검색어 없는 '전체/필터' 목록에 효과.

## 검증
- 스키마 변경이라 admin-server 빌드/테스트(H2 Flyway 적용)로 마이그레이션 적용 검증. 컬럼·테이블·버전(V39)·인덱스명 충돌 없음 확인.

## Git/PR
- 브랜치 `feature/perf-list-query-indexes` → PR 대상 `dev`. 파일: V39 마이그레이션 1개.
