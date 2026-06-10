# 리포트 — Strangler 마무리: 모놀리식 root 소스 제거 (2026-06-10)

- 작성자: Lead 강태오
- 브랜치: `feature/strangler-remove-monolith-src` → `dev`
- 워크플로우: `workflows/2026-06-10_strangler-remove-monolith-src.md`

## 결과 요약

| 항목 | 결과 | 비고 |
|---|---|---|
| 모놀리식 root 소스 제거 | ✅ `qtai-server/src` 749파일 삭제 | 도메인·스키마·툴 모두 모듈에 존재(검증 완료) |
| 모놀리식 Dockerfile 제거 | ✅ `qtai-server/Dockerfile` | 모듈별 Dockerfile·compose·k8s가 대체 |
| 루트 빌드 정리 | ✅ aggregator(`base` 플러그인)로 축소 | bootJar·AI JavaExec 태스크 제거 |
| settings 주석 갱신 | ✅ "추출 완료" 반영 | — |
| 빌드 검증 | ✅ `build -x test` 성공 | 6모듈 27 tasks |
| 테스트 검증 | ✅ `test` 성공 | 2m 18s, 전 모듈 통과 |

## 변경 내역

- 삭제: `qtai-server/src/**`(749), `qtai-server/Dockerfile`
- 수정: `qtai-server/build.gradle.kts`(aggregator화), `qtai-server/settings.gradle.kts`(주석)
- 신규 문서: 본 리포트 + 워크플로우

## 검증 로그

```
./gradlew --no-daemon build -x test  → BUILD SUCCESSFUL (27 actionable tasks)
./gradlew --no-daemon test           → BUILD SUCCESSFUL in 2m 18s
```

## 남은 작업 (후속)

1. dev → master 승격 (admin-web 재취합 리포트 남은작업 ②).
2. AI 진단 툴 3종이 필요하면 `service-ai`/`admin-server`에서 JavaExec 태스크 재등록.
3. 디스크 잔여 폴더(`ai-service` 등 git 미추적) 로컬 정리.

## 비고

- PR 규모(750파일+)는 모놀리식 일괄 제거 특성상 분할 불가 — PR 본문에 사유 명시.
- 되돌리기: 커밋 revert + 백업 태그 `archive/dev-msa-before-reset-20260609`.
