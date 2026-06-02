# 2026-06-01 나눔 피드(S-01) + 상세(S-02) + 홈 하단 탭바 구현

## 목표
나눔 피드 목록/상세 화면과 홈 하단 탭바(홈/나눔/마이)를 구현한다.

## 작업 내용
1. **SharingPostItem/SharingPostDetail 모델** — 서버 응답 파싱
2. **SharingRepository** — GET /sharing-posts, GET /sharing-posts/{id}
3. **sharingPostsProvider** — 카테고리 필터 + 검색어 연동
4. **SharingFeedScreen (S-01)** — 나눔 목록 + 카테고리 필터(6종) + 검색
5. **SharingDetailScreen (S-02)** — 상세 본문 + 좋아요/댓글/신고 UI (API는 TODO)
6. **HomeScreen** — BottomNavigationBar 3탭 (홈/나눔/마이) + IndexedStack
7. **AppRouter** — /sharing, /sharing/detail 라우트, /home → HomeScreen 교체

## 범위
- 브랜치: `feature/flutter-sharing`
- 변경: 9파일 573줄
- 관련: F-10 닉네임 나눔 공간

## 미해결
- PR 머지 대기
- 좋아요/댓글/신고 API — 김지민 파트 백엔드 완성 후 연결

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업)
