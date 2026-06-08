# 2026-06-08 나눔 상세 화면 위젯 컴포넌트화

## 목표
비대해진 나눔 상세 화면(369줄)의 재사용 가능한 UI 조각을 도메인 `widgets/`로 분리해 가독성·재사용성을 높인다. 동작·UI 변경 없는 순수 구조 리팩토링.

## 작업 내용
1. **sharing/widgets/ 신설** — 도메인 전용 위젯 폴더 추가 (기존 sharing은 widgets 폴더 부재)
2. **SharingSnapshotCard** — 작성자 아바타/닉네임/카테고리/제목 요약 카드 (화면 내부 `_SnapshotCard` 승격)
3. **SharingCommentTile** — 댓글 1건 타일 (`onDelete`가 null이면 삭제 버튼 미표시 → 내 댓글일 때만 전달)
4. **SharingCommentInput** — 댓글 입력 줄(텍스트 필드 + 전송 버튼), 입력값/전송중 상태는 상위 화면이 소유
5. **sharing_detail_screen** — 위 위젯 사용으로 369→286줄 축소, 동작 동일. 스냅샷 카드에 `AppGap`/`AppRadius` 토큰 적용

## 범위
- 브랜치: `refactor/flutter-sharing-widgets` (base: `dev`)
- 변경: 4파일 (+157/-94) — 신규 위젯 3, 화면 1
- 관련: F-10 닉네임 나눔 공간 (S-02 나눔 상세)

## 검증
- `flutter analyze lib` — No issues found

## 미해결
- PR 머지 대기
- 좋아요/댓글 수 행, 신고 시트 등 추가 분리는 후속(필요 시)

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
