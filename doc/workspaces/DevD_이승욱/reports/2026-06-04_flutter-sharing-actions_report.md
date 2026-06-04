# 2026-06-04 나눔 좋아요/삭제 + 스냅샷 카드 — 결과 보고

## 요약
나눔 상세 화면에서 좋아요 토글(POST/DELETE) + 본인 글 삭제(DELETE) API 연결 + 스냅샷 카드 위젯 추가. 본 PR은 코드 2파일 + 문서 2파일을 포함한다.

## 산출물

| 파일 | 설명 |
|------|------|
| `sharing_repository.dart` | like/unlike, deletePost, publishNote 4개 메서드 추가 |
| `sharing_detail_screen.dart` | 좋아요 토글 + 삭제 다이얼로그 + SnapshotCard 위젯 |

## 정책 확인
- **공유 스냅샷 정책**: 공유 시점의 닉네임/제목/본문/카테고리를 복사. 원본 변경에 영향받지 않음 (07 §F-10)
- **검증 통과 콘텐츠 노출**: 나눔 목록은 PUBLISHED 상태만 조회 (서버 SharingPostService에서 필터)
- **confirmNicknamePublic=true 필수**: publishNote에서 서버에 동의값 전달

## 검증
- `flutter analyze` — No issues found
- `flutter test` — 101건 통과

## 미해결
- PR 머지 대기
- 댓글/신고 API — 김지민 파트 백엔드 완성 후 연결
