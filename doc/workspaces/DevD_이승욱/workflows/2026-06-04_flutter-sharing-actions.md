# 2026-06-04 나눔 좋아요/삭제 API 연결 + 스냅샷 카드 UI (S-03)

## 목표
나눔 상세 화면에서 좋아요 토글/삭제 API를 연결하고 스냅샷 카드 위젯을 추가한다.

## 작업 내용
1. **SharingRepository** — like/unlike, deletePost, publishNote API 메서드 추가
2. **SharingDetailScreen** — 좋아요 토글 API 연결 + 본인 글 삭제(확인 다이얼로그) + 신고 TODO
3. **SnapshotCard 위젯** — 작성자 아바타/닉네임/카테고리/제목 요약 카드

## 범위
- 브랜치: `feature/flutter-sharing-actions`
- 변경: 2파일 196줄
- 관련: F-10 닉네임 나눔 공간

## 미해결
- PR 머지 대기
- 댓글/신고 API — 김지민 파트

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
