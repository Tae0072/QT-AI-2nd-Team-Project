# 워크플로우 — report 신고 대상 존재성 검증 (B)

- 작업자: Lead 강태오
- 날짜: 2026-05-29
- 기준: `04_API_명세서.md` §4.4.7, `CLAUDE.md` §3~§5

## 1. 배경

#140에서 신고 접수(POST /api/v1/reports)는 형식·중복 검증까지만 구현했고, 대상의 실제 존재 검증은 후속 과제였다. 본 작업으로 대상 도메인 api를 통한 존재 검증을 보강한다.

## 2. 절차

1. sharing/ai api 가용성 확인: POST=`GetSharingPostUseCase.getDetail`(구현됨), AI_QA_REQUEST=`GetAiQaResultUseCase.getAiQaResult`(구현됨). COMMENT(CommentUseCase TODO)·AI_ASSET(사용자용 조회 없음)은 검증 불가.
2. `feature/report-target-validation` 브랜치
3. `ReportService`에 `validateTargetExists` 추가 — 가능한 2종 검증, NOT_FOUND/FORBIDDEN → `REPORT_TARGET_NOT_FOUND` 변환
4. `ErrorCode.REPORT_TARGET_NOT_FOUND` 추가
5. 단위 테스트(대상 없음 2종) + 전체 회귀 통과 후 PR

## 3. 검증

```powershell
cd qtai-server
.\gradlew.bat --stop ; Remove-Item build\classes,build\resources,build\test-results,build\tmp -Recurse -Force
.\gradlew.bat test --no-daemon --console=plain
```
