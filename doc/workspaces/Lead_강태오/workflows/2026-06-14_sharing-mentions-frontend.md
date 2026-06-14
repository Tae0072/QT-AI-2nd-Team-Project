# 2026-06-14 나눔 '#닉네임' 멘션(태그) — 프론트(PR B)

PR A(백엔드)에 이어 사용자 화면을 붙인다.

## 요청
- 저장 버튼 옆 **'#태그' 버튼** → 내가 태그된 글만 보기.
- 글·댓글에 `#닉네임`으로 태그(자동완성).

## 구현(Flutter)
- `SharingRepository.getMentions()`(GET /me/mentions), `searchMembers(q)`(GET /members/search) + `MemberSuggestion` 모델.
- `mentionsProvider`(FutureProvider) — 내가 태그된 글.
- 나눔 피드 카테고리 줄 오른쪽에 **저장 / #태그** 두 버튼(`_FeedActionButton`). #태그 → `/sharing/mentions`.
- `SharingMentionsScreen`: 태그된 글 목록(PostCard 재사용, 빈 상태 안내, 보기 전용).
- 댓글 입력 **자동완성**: 입력 중 커서 앞이 `#닉네임`으로 끝나면 200ms 디바운스로 회원 검색 → 후보 리스트 표시, 선택 시 `#닉네임 ` 삽입(상세 화면 상태에서 처리).

## 검증
- `flutter analyze` 무이슈, `flutter test` 전체 **316개 통과**(신규 `SharingMentionsScreen` 위젯 테스트 2건 포함).

## 운영 메모(중요)
- 멘션 엔드포인트가 동작하려면 **로컬 백엔드 컨테이너를 새 이미지로 재빌드**해야 한다(service-note·service-user). 머지만으로는 실행 중 컨테이너에 반영되지 않음 → `scripts/dev-up.ps1`(또는 compose build) 재실행 필요.
- 멘션 알림은 좋아요·댓글과 동일하게 통합 대기(Mock). '내가 태그된 글' 목록·자동완성은 재빌드 후 즉시 동작.

## Git/PR
- 브랜치 `feature/sharing-mentions-fe` → PR `dev`. 커밋 `feat(sharing): #태그 버튼·태그된 글 목록·댓글 멘션 자동완성(FE)`.
