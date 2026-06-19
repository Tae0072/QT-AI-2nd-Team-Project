# 리포트 — 관리자 웹 목업(데모) 모드

- 날짜: 2026-06-19
- 담당: Lead 강태오
- 브랜치: `feature/admin-web-mock-mode` → PR 대상 `dev`

## 무엇을 했나

`admin-web` 의 모든 메뉴(AD-01~20, 18개 화면)가 **백엔드 없이** 목업 데이터로 동작하도록 axios 어댑터 기반 인메모리 목업 계층을 추가했다. `npm run dev:mock` 한 줄로 시연 가능.

## 동작 범위(메뉴별)

| 메뉴 | 경로 | 목업 동작 |
| --- | --- | --- |
| 대시보드 (AD-01) | /dashboard | 요약 지표·오늘 QT(READY)·최근 활동 |
| 오늘 QT 관리 (AD-02) | /qt-passages | 목록/등록/게시/숨김/수정 |
| AI 산출물 검증 (AD-03) | /ai-assets | 목록/상세/승인·반려·숨김·재생성 |
| 신고 처리 (AD-04) | /reports | 목록/처리·반려/테스트 시드 |
| 회원 관리 (AD-13) | /members | 목록/상세/하위목록/정지·해제 |
| 미션 관리 (AD-16) | /missions | 목록/생성/상태토글/수정 |
| 나눔 공유글 관리 (AD-15) | /sharing-posts | 목록/상세/숨김·복원 |
| 시스템 공지 (AD-06) | /notices | 목록/상세/작성/수정/발행/숨김 |
| 배경음악 관리 (AD-12) | /music-tracks | 목록/등록/수정/공개·숨김/삭제 |
| 감사 로그 (AD-07) | /audit-logs | 목록 |
| AI 운영 모니터링 (AD-08) | /ai-monitoring | 생성/검증/배치/Q&A 요약 |
| AI 검증 체크리스트 (AD-09) | /ai-checklists | 목록/생성/활성·폐기 |
| AI 배치 실행 로그 (AD-10) | /ai-batch-logs | 목록 |
| AI 평가 세트 (AD-11) | /ai-evaluations | 세트/케이스/실행(latest) |
| 시뮬레이터 관리 (AD-14) | /simulator-clips | 목록/숨김 |
| AI 프롬프트 관리 | /ai-prompt-versions | 목록/상세/생성/활성·폐기 |
| QT 영상 관리 (AD-20) | /qt-videos | 원본/구간/클립/성경권 |
| 자가진단 (AD-18) | /self-test | 전체 점검 16/16 성공 |

## 검증 결과

- 타입체크/빌드: `npx tsc --noEmit`, `npm run build` 모두 통과.
- 런타임(Chrome 스모크): 대시보드·회원 관리·QT 영상·AI 평가 세트·자가진단 정상 렌더.
- 자가진단 전체 점검: **16/16 성공, 권한가드 0, 실패 0**.

## 안전성

- 일반 `vite`/`vite build` 에서는 비활성(MODE 가 mock 이 아님) → 운영/일반 dev 영향 0.
- DB·서버 무변경(메모리만). 시연 후 새로고침이면 초기 시드로 복귀.

## 남은 사항(비차단)

- `scripts/admin-page-contracts.test.mjs` 가 삭제된 `PraiseSongsPage.tsx` 참조로 실패 — 본 작업과 무관한 기존 결함. 별도 정리 권장.
- 운영(실데이터) 연동 시에는 본 모드를 끄고 실제 admin-server(8090)·각 서비스로 호출.
