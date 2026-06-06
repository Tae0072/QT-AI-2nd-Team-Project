# 2026-06-01 찬양 화면(M-03) Flutter 구현 (F-09)

## 목표
마이페이지 퀵메뉴 찬양 탭에서 진입하는 찬양 화면(내 찬양 + 큐레이션 TabBar)을 구현한다.

## 작업 내용
1. **PraiseSong/MyPraiseSong 모델** — 서버 응답 파싱
2. **MyPageRepository** — 찬양 API 4개 메서드 추가 (큐레이션 조회, 내 찬양 조회/저장/삭제)
3. **curationSongsProvider / myPraiseSongsProvider** — Riverpod FutureProvider
4. **PraiseScreen** — TabBar 2개 (내 찬양 삭제 + 큐레이션 저장)
5. **AppRouter** — `/praise` 라우트 + 마이페이지 퀵메뉴 찬양 탭 와이어링

## 범위
- 브랜치: `feature/flutter-praise`
- 변경: 6파일 257줄
- 관련: F-09 찬양 큐레이션

## 미해결
- PR 머지 대기

## 담당
- Lead 강태오 (T)
