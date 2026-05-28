# 2026-05-28 ArchUnit 도메인 경계 자동 검증 테스트

## 목표
CLAUDE.md §3(도메인 경계)과 §4(패키지·레이어)를 ArchUnit으로 자동 검증해, 도메인 간 금지 import가 빌드 시점에 잡히도록 한다.

## 작업 순서

| # | 작업 | 상태 |
|---|------|------|
| 1 | 현재 ArchUnit 설정 및 도메인 구조 파악 | 완료 |
| 2 | DomainBoundaryArchTest 작성 (29개 규칙) | 완료 |
| 3 | 1차 빌드 → 2개 규칙 실패 원인 분석 | 완료 |
| 4 | 규칙 4 삭제 + 규칙 2 per-domain 방식 재작성 | 완료 |
| 5 | 빌드·테스트 검증 (2회) | 완료 |
| 6 | 커밋·푸시·PR #136 생성 | 완료 |
| 7 | 워크플로우·리포트 작성 | 완료 |

## 핵심 결정
- 규칙 4(전체 internal cross-domain)는 13개 per-domain 규칙과 중복 + 같은 도메인 내부 참조를 false positive로 잡아 삭제
- 규칙 2(web 교차 의존)는 per-domain 방식으로 재작성해 같은 도메인 내 참조를 자연스럽게 허용
- `@AnalyzeClasses(importOptions = DoNotIncludeTests.class)`로 테스트 코드는 검증 대상에서 제외
