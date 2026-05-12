# OAuth + dev 브랜치 자동 머지 테스트

이 파일은 아래 전체 흐름을 검증하는 테스트용입니다.

1. feature/* → dev PR 오픈
2. Claude 자동 리뷰 (OAuth 구독 방식)
3. CI 전체 통과
4. squash merge → dev 자동 완료

테스트 시각: 2026-05-12
