# 2026-05-28 학습 갭 분석 및 study-notes 작성

## 목표
노션 기술 블로그와 Lead 워크스페이스를 비교하여, QT-AI 프로젝트에서 사용하지만 아직 배우지 않은 기술/방법을 개별 학습 노트로 작성한다.

## 작업 순서

| # | 작업 | 상태 |
|---|------|------|
| 1 | 노션 기술 블로그 전체 커리큘럼 수집 (Notion MCP) | 완료 |
| 2 | Lead 워크스페이스 reports/workflows 분석 | 완료 |
| 3 | QT-AI 프로젝트 실제 사용 기술 스택 파악 | 완료 |
| 4 | 노션 커리큘럼 vs 프로젝트 스택 비교 → 갭 도출 | 완료 |
| 5 | 갭 항목별 개별 .md 파일 작성 (입문자 기준) | 완료 |
| 6 | 결과 검증 (파일 수, 내용, 구조) | 완료 |
| 7 | 워크플로우 + 리포트 작성 | 완료 |

## 비교 기준

### 노션에서 이미 배운 것
- Java: 기초, 람다, 제네릭, 컬렉션, 스레드
- Network: 소켓 프로그래밍
- DB: SQL, 트랜잭션, 인덱스, BST/AVL
- 웹 프론트: HTML, CSS, JS
- Spring: 서블릿, v1-v4, ORM, Security, FilterChain, CSRF, CORS, Nginx, OSIV, REST API, SSE, DI, SOLID
- MSA: Docker, Kubernetes, Kafka, WebSocket, Flutter/Dart/Riverpod/MVVM, DDD
- Git: 기초, merge, rebase, reset

### QT-AI 프로젝트에서 사용하는 것
- Java 21, Spring Boot 3.3, Gradle Kotlin DSL
- Spring Modulith, ArchUnit
- JWT 인증, Caffeine Cache
- JUnit 5 + Spring Test, H2 DB
- GitHub Actions CI/CD
- OpenAPI + Spectral, JaCoCo, Gitleaks
- Docker Compose
- Spring ApplicationEventPublisher
- Conventional Commits

## 산출물
- `study-notes/README.md`: 전체 목록과 프로젝트 사용 위치
- `study-notes/01_spring-boot-3.md` ~ `study-notes/15_jacoco.md`: 개별 학습 노트 15개
