# 2026-05-27 Controller @PathVariable 바인딩 수정 — 결과 보고

## 요약
Spring Boot 3.3에서 @PathVariable 자동 매핑 실패 문제 수정. 명시적 이름 추가 + `-parameters` 컴파일러 플래그 설정. PR #106 Merged.

## 산출물

| 파일 | 설명 |
|------|------|
| Controller 파일들 | `@PathVariable("id") Long id` 명시적 이름 추가 |
| `build.gradle.kts` | `options.compilerArgs.add("-parameters")` 추가 |

## 검증
- `gradlew compileJava` — BUILD SUCCESSFUL
- `gradlew test` — 전체 통과

## 미해결
- 없음
