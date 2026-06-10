# 2026-06-10 Strangler 마무리 — 모놀리식 root 소스 제거

- 작성: 강태오(Lead, AI 보조)
- 대상 저장소: 구현 `QT-AI-2nd-Team-Project`
- 브랜치: `feature/strangler-remove-monolith-src` → PR 대상 `dev`
- 근거: `reports/2026-06-10_admin-web-reintegration-report.md` 남은작업 ①, `reports/2026-06-10_msa-transition-status.md` §5, `CLAUDE.md` §3·§4

## 1. 배경

MSA 멀티모듈(Strangler) 전환에서 도메인 추출이 끝났다. 6개 모듈(`lib-common`, `service-user`, `service-bible`, `service-note`, `service-ai`, `admin-server`)이 사용자 기능과 관리자 기능을 모두 담고 있고, 옛 모놀리식 root 소스(`qtai-server/src`)는 추출이 진행되는 동안만 무손상으로 남겨둔 임시 상태였다. 이번 작업은 그 root 소스를 제거해 Strangler를 마무리한다.

## 2. 제거 전 안전성 검증 (지웠을 때 빠지는 것이 없는지)

| 항목 | root | 이전처(모듈) | 결론 |
|------|------|--------------|------|
| 도메인 코드(ai 예시) | 157 파일 | service-ai 168 + admin-server 복사본 | 모듈이 상위집합 |
| Flyway 마이그레이션(단일 DB 스키마) | 30개 | admin-server V1~V32 + service-bible | 누락 없음 |
| AI 진단 JavaExec 툴 클래스 3종 | 있음 | admin-server·service-ai에 동일 클래스 존재 | 누락 없음 |
| 컨테이너 빌드 | root `Dockerfile`(모놀리식 bootJar) | 모듈별 `Dockerfile` 5종 + compose/k8s | root 미참조 → 제거 가능 |

`qt_video_clips` 스키마는 admin-server `V32`·service-bible `V30`에 존재한다(root의 `V30`은 옛 복사본).

## 3. 작업 범위

1. 삭제: `qtai-server/src/`(749 파일), `qtai-server/Dockerfile`(모놀리식 bootJar 전용).
2. 수정: `qtai-server/build.gradle.kts` → Spring Boot 애플리케이션 플러그인·모놀리식 의존성·AI JavaExec 태스크 3개를 제거하고, `base` 플러그인만 두는 **순수 aggregator**로 축소.
3. 수정: `qtai-server/settings.gradle.kts` 주석을 "추출 완료" 상태로 갱신.
4. 유지: `apis/`(OpenAPI 계약), `data/`, gradle 래퍼, 6개 모듈 — 모놀리식 전용이 아님.

> 참고: AI 진단 툴 3종은 더 이상 root 태스크로 실행할 수 없다. 필요해지면 `service-ai` 또는 `admin-server` 모듈에서 동일 클래스로 JavaExec 태스크를 재등록한다(후속).

## 4. 검증 (로컬, Windows JDK21 / Gradle 9.5.1)

```
cd qtai-server
./gradlew --no-daemon build -x test   → BUILD SUCCESSFUL (6모듈, 27 tasks)
./gradlew --no-daemon test            → BUILD SUCCESSFUL (2m 18s, 전 모듈)
```

root 소스 없이 6개 모듈이 모두 컴파일·bootJar·테스트까지 통과. 회귀 없음.

## 5. 유의

- 이 PR은 모놀리식 트리 일괄 삭제라 변경 파일 수가 750개를 크게 넘는다(`09_Git_규칙` 권고 10파일/500줄 초과). Strangler 제거는 원자적 작업이라 분할이 무의미하므로 PR 본문에 사유를 남긴다.
- 되돌리기: 단일 커밋 revert로 복구 가능하고, 폐기분 백업 태그 `archive/dev-msa-before-reset-20260609`도 별도로 보존돼 있다.
- 작업 중 발견한 디스크 잔여 폴더(`ai-service`, `bible-service`, `service-gateway`, `admin-service`, `lib-common-web`)는 git 미추적 찌꺼기로 이번 PR과 무관하며, 로컬 정리는 별도로 처리한다.
