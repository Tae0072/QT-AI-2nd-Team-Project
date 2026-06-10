# 2026-06-06 P2 — MSA 준비 정리 워크플로우

리뷰 §4(죽은 코드)·§5.2 #1(SQL 경계) 1차 정리의 머지 가이드.
결과: `reports/2026-06-06_p2-msa-prep-cleanup_report.md`

## 0. 전제

- 구현·커밋 완료. **push·PR 생성은 사용자(T)가 직접.** 모든 작업 브랜치는 `origin/dev` 분기, PR 대상 `dev`.
- 삭제는 "프로덕션 주입처 0건" 재검증(서브에이전트 usage 스윕 + grep + 삭제 후 전체 빌드) 후에만 수행.

## 1. 브랜치 → 내용(커밋)

| 브랜치 | 커밋 | 파일 | 성격 |
|--------|------|------|------|
| `chore/remove-unwired-client-stubs` | 3cca2ce | 10 삭제 | 미와이어드 client Mock·external/kakao·praise 스텁 |
| `chore/remove-dead-dtos` | 72145f2 | 5 삭제 | 죽은 DTO·스텁 엔티티(qt/member/ai) |
| `refactor/qt-bible-sql-boundary` | e48d4c2 | 12 변경(+232/−231) | qt→bible SQL 경계 제거(§5.2 #1) |

## 2. 충돌/의존

- 세 브랜치는 **파일 교집합 없음** → 머지 순서 자유.
- `refactor/qt-bible-sql-boundary`는 bible `ListBibleBooksUseCase`(기존 포트) 주입만 추가 — bible 도메인 변경 0, ArchUnit 통과.
- 주의: `JournalEventRepository`의 미사용 메서드 2개 정리는 **이 배치에 포함하지 않음**(P1-10 `fix/journal-events-outbox`와 같은 파일 → 충돌 방지). P1-10 PR에 합치거나 그 머지 후 정리.

## 3. 권장 머지 순서

1. `chore/remove-unwired-client-stubs`
2. `chore/remove-dead-dtos`
3. `refactor/qt-bible-sql-boundary`

(순서 자유지만 삭제 2건 먼저 → 리팩터링 순이 리뷰가 깔끔.) 각 PR 본문에 리뷰 리포트 절(§4 / §5.2 #1)과 워크플로우 경로를 명시.

## 4. 검증

```bash
$env:JAVA_HOME='D:\workspace\tools\jdk\jdk-21.0.11+10'
cd D:\workspace\QT-AI-2nd-Team-Project\qtai-server
Remove-Item build\test-results -Recurse -Force -ErrorAction SilentlyContinue
.\gradlew.bat --no-daemon test
```

- 현 시점 전체 스위트 **831 tests / 0 failures / 4 skipped**(Docker 필요 테스트는 skip).
- 삭제 PR은 컴파일·전체 테스트로 "참조 0건"이 자동 증명됨(끊긴 참조가 있으면 빌드 실패).

## 5. 주의

- 보류 항목(ai/client/qt 포트, 헬퍼 중복 추출, JournalEventRepository 미사용 메서드)은 리포트의 "의도적으로 보류한 항목" 참고 — 제품 결정/머지 순서 정리 후 별도 진행.
- `.gradle`/build output/coverage 등 stage 금지(CLAUDE.md §12).
