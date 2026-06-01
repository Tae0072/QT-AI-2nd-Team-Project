# 2026-06-01 AiGenerationJobRunner 생성자 접근제한자 수정 — 결과 보고

## 요약
dev 브랜치에서 `AiGenerationJobRunner`의 클래스·생성자가 package-private이어서 Spring 빈 생성 실패(`No default constructor found`) → 서버 기동 불가. 클래스를 `public`으로, Spring 주입 생성자에 `@Autowired public`을 추가하여 해결.

## 산출물

| 파일 | 설명 |
|------|------|
| `AiGenerationJobRunner.java` | 클래스 `public` + 생성자 `@Autowired public` 추가 (1파일 3줄) |

## 원인 분석

| 에러 | 원인 PR | 담당 |
|------|---------|------|
| `No default constructor found` — Spring 빈 생성 실패 | #155/#156 feat(ai) | 강상민 (DevC) |

## 검증
- `gradlew clean bootRun` — 정상 기동 확인
- `POST /api/v1/auth/kakao` — 정상 응답 확인

## 미해결
- PR 머지 대기
