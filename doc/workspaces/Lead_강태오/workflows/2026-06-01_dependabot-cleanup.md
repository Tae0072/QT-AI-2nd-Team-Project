# 워크플로우 — dependabot PR 정리(Feature Freeze)

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project
- 기준: 시연 일정(Feature Freeze), `CLAUDE.md` §8 금지 기술
- 대상 PR: #157~#163 (닫음)

## 1. 배경

자동 의존성 봇(dependabot)이 만든 PR 7건(#157~#163)이 열려 있었다. 시연이 임박한 Feature Freeze 기간에 불필요한 변경/리스크를 줄여야 한다.

## 2. 평가

- #157·158·160·161·163 — GitHub Actions 버전 bump(CI). Freeze 중 불필요.
- #159 — spring-dependency-management 패치. 사소·불필요.
- **#162 — Spring Boot 3.3.4 → 4.0.6(메이저)**. Spring Framework 7/Jakarta EE 11 breaking → 시연 전 도입 절대 금지.
- 전부 `master` 대상이라 dev 플로우와도 불일치.

→ 결론: 7건 모두 "꼭 해야 하는 것" 아님 → 닫음.

## 3. 절차

1. 각 PR 메타 확인(작성자=dependabot, base=master, 변경 규모).
2. #162에 위험 사유 코멘트(메이저 보류).
3. 7건 모두 `gh pr close` + "Freeze 기간, 시연 이후 일괄 검토" 사유.
4. (권장) 시연까지 `.github/dependabot.yml` 일시중지/보안-only 설정 — 재제안 방지.

## 4. 검증 명령

```powershell
gh pr view <n> --json author,baseRefName,title
gh pr close <n> --comment "..."
gh pr list --state open
```
