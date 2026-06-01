# 2026-06-01 AiGenerationJobRunner 생성자 접근제한자 수정

## 목표
dev 브랜치에서 `AiGenerationJobRunner` 빈 생성 실패로 서버 기동 불가 문제를 해결한다.

## 작업 내용
1. **클래스 접근제한자 수정** — `class AiGenerationJobRunner` → `public class AiGenerationJobRunner` (Spring이 패키지 외부에서 빈 생성 가능하도록)
2. **생성자 접근제한자 + @Autowired** — 다중 생성자(Spring 주입용 5파라미터 + 테스트용 6파라미터) 중 Spring 주입 대상에 `@Autowired public` 명시

## 범위
- 브랜치: `bugfix/ai-runner-constructor`
- PR: (Open, 리뷰 대기)
- 커밋: `fix(ai): AiGenerationJobRunner 생성자 접근제한자 수정`
- 변경: 1파일 3줄 (`AiGenerationJobRunner.java`)
- 비고: 강상민(DevC) AI 도메인 코드 — dev 전체 기동 불가 긴급 수정

## 미해결
- PR 머지 대기

## 담당
- Lead 강태오 (T)
