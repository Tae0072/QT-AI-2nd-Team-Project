# Report — 2026-05-28 학습 갭 분석 및 study-notes 작성

| 항목 | 내용 |
| --- | --- |
| 담당자 | Lead_강태오 |
| 작업일 | 2026-05-28 |
| 관련 워크플로우 | `workflows/2026-05-28_study-notes-gap-analysis.md` |

## 요약
노션 기술 블로그(9개 카테고리, 150+ 학습 항목)와 QT-AI 프로젝트 실제 사용 기술을 비교하여, 아직 배우지 않은 15개 기술/방법을 개별 학습 노트로 작성했다.

## 비교 결과

### 노션에서 이미 커버된 영역 (프로젝트에도 사용)
- Java 기초 문법 → QT-AI 서버 전체
- SQL, 트랜잭션, 인덱스 → MySQL 8.0 운영 DB
- Spring Security, FilterChain → 인증/인가 구조
- REST API 개념 → `/api/v1/**` 엔드포인트
- Docker 기초 → 컨테이너 빌드/실행
- Git merge/rebase → 브랜치 관리
- Flutter/Dart/Riverpod → 사용자 앱

### 갭으로 도출된 15개 기술

| # | 기술 | 분류 | 프로젝트 필요도 |
|---|------|------|----------------|
| 1 | Spring Boot 3.x | 프레임워크 | 필수 (서버 전체) |
| 2 | Spring Modulith | 아키텍처 | 필수 (도메인 경계) |
| 3 | ArchUnit | 테스트/품질 | 필수 (import 금지 검증) |
| 4 | Gradle Kotlin DSL | 빌드 도구 | 필수 (빌드 설정) |
| 5 | JUnit 5 + Spring Test | 테스트 | 필수 (전체 테스트) |
| 6 | JWT 인증 | 보안 | 필수 (로그인 유지) |
| 7 | H2 Database | 테스트 DB | 높음 (테스트 환경) |
| 8 | Caffeine Cache | 캐시 | 높음 (Today QT) |
| 9 | GitHub Actions | CI/CD | 높음 (자동 빌드/리뷰) |
| 10 | OpenAPI + Spectral | API 품질 | 높음 (API 검증) |
| 11 | Docker Compose | 인프라 | 높음 (배포 환경) |
| 12 | Spring Event Publisher | 아키텍처 | 높음 (도메인 간 통신) |
| 13 | Conventional Commits | Git 규칙 | 중간 (커밋 메시지) |
| 14 | Gitleaks | 보안 도구 | 중간 (시크릿 검사) |
| 15 | JaCoCo | 품질 도구 | 중간 (커버리지) |

## 산출물

| 파일 | 경로 |
|------|------|
| README | `study-notes/README.md` |
| 학습 노트 15개 | `study-notes/01_spring-boot-3.md` ~ `15_jacoco.md` |
| 총 줄 수 | 1,621줄 (16개 파일) |

## 검증 결과

- 파일 수: 16개 (README 1 + 학습 노트 15) ✅
- 각 파일에 "왜 배워야 하나" 설명 포함 ✅
- 모든 코드 예시가 QT-AI 프로젝트 맥락에 맞춤 ✅
- 입문자 기준 설명 (전문 용어 사용 시 즉시 풀어서 설명) ✅
- CLAUDE.md 금지 기술(Kafka, K8s, Helm, RAG 등)은 학습 노트 대상이 아님 ✅
