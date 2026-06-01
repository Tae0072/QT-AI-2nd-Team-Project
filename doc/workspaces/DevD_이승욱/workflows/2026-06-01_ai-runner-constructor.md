# 2026-06-01 AiGenerationJobRunner 다중 생성자 @Autowired 명시

## 목표
dev 브랜치에서 `AiGenerationJobRunner` 빈 생성 실패(`No default constructor found`)로 서버 기동 불가 문제를 해결한다.

## 작업 내용
1. **Spring 주입 생성자에 `@Autowired` 추가** — 생성자가 2개(Spring 주입용 5파라미터 + 테스트용 6파라미터)인데 `@Autowired`가 없어 Spring이 기본 생성자를 찾다 실패. 주입 대상 생성자에 `@Autowired`를 명시하여 해결
2. **클래스 package-private 유지** — `internal` 패키지 Modular Monolith 경계 보존. Spring `@ComponentScan`은 같은 패키지 루트 하위를 스캔하므로 package-private도 빈 생성 가능

## 범위
- 브랜치: `bugfix/ai-runner-constructor`
- PR: (Open, 리뷰 대기)
- 커밋: `fix(ai): AiGenerationJobRunner 다중 생성자 @Autowired 명시`
- 변경: 1파일 (`AiGenerationJobRunner.java` — `@Autowired` 1줄 추가)
- 비고: 강상민(DevC) AI 도메인 코드 — dev 전체 기동 불가 긴급 수정

## 미해결
- PR 머지 대기

## 담당
- Lead 강태오 (T)
