# 2026-05-27 Controller @PathVariable 바인딩 수정

## 목표
Spring Boot 3.3에서 `-parameters` 컴파일 플래그 없이 @PathVariable 이름이 자동 매핑되지 않는 문제를 수정한다. Controller의 @PathVariable에 명시적 이름을 추가하고, build.gradle.kts에 `-parameters` 컴파일러 플래그를 설정한다.

## 작업 내용
1. **@PathVariable 명시적 이름 추가** — `@PathVariable Long id` → `@PathVariable("id") Long id` 패턴으로 변경
2. **build.gradle.kts `-parameters` 플래그** — `options.compilerArgs.add("-parameters")` 추가

## 범위
- 브랜치: `bugfix/controller-pathvariable-binding`
- PR: #106 (Merged)

## 미해결
- 없음

## 담당
- DevD 이승욱
