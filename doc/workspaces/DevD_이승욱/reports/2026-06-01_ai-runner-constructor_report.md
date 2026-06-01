# 2026-06-01 AiGenerationJobRunner 다중 생성자 @Autowired 명시 — 결과 보고

## 요약
dev 브랜치에서 `AiGenerationJobRunner`에 생성자가 2개인데 `@Autowired`가 없어 Spring이 기본 생성자를 찾다 `No default constructor found` 에러로 서버 기동 실패. Spring 주입 생성자에 `@Autowired`만 추가하여 해결. 클래스는 package-private 유지 — Modular Monolith `internal` 경계 보존.

## 산출물

| 파일 | 설명 |
|------|------|
| `AiGenerationJobRunner.java` | Spring 주입 생성자에 `@Autowired` 추가. 클래스 package-private 유지 |

## 원인 분석

| 에러 | 원인 | 담당 |
|------|------|------|
| `No default constructor found` | 생성자 2개(5파라미터 + 6파라미터)에 `@Autowired` 미지정 → Spring이 기본 생성자를 찾다 실패 | 강상민 (DevC) |

## 검증
- `gradlew clean bootRun` — 정상 기동 확인
- `POST /api/v1/auth/kakao` — 정상 응답 확인

## 미해결
- PR 머지 대기
