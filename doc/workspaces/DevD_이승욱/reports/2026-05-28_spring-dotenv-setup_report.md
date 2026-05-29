# 2026-05-28 spring-dotenv 설정 — 결과 보고

## 요약
`spring-dotenv:4.0.0`을 `developmentOnly` 스코프로 추가하여 로컬 개발 시 `.env` 파일에서 환경변수(JWT 키, DB 접속 등)를 자동 로딩하도록 구현. PR 리뷰에서 `implementation` → `developmentOnly` 스코프 변경 요청 반영 후 Merged.

## 산출물

| 파일 | 설명 |
|------|------|
| `qtai-server/build.gradle.kts` | `me.paulschwarz:spring-dotenv:4.0.0` `developmentOnly` 추가 |
| `.gitignore` | `.env` 패턴 추가 — 민감 정보 커밋 방지 |

## 검증
- `Started QtAiApplication in 7.93 seconds` — 서버 정상 시작 확인
- `.env` 파일 gitignore 적용 확인
- `developmentOnly` 스코프 — 운영 런타임에 `.env` 자동 로딩 비활성화 확인

## 리뷰 이력
- 1차 리뷰: `implementation` → `developmentOnly` 스코프 변경 요청 → 수정 후 amend + force push
- Merged (#131)

## 미해결
- `.env.example` 파일 추가는 후속 PR에서 처리 가능
