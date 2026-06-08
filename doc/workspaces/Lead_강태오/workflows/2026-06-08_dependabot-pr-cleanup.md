# 워크플로우 — dependabot PR 정리(#320~#327)

- 작업자: Lead 강태오
- 날짜: 2026-06-08
- 대상 저장소: QT-AI-2nd-Team-Project
- 기준: MVP 데모 안정성, `CLAUDE.md` §12 Git 규칙(PR 대상=dev), §8 금지 기술
- 대상 PR: #320~#327 (평가) → #322~#327 닫음 / #320·#321 유지

## 1. 배경

자동 의존성 봇(dependabot) PR 8건(#320~#327)이 06-08 동시에 열렸다. 전부 `master` 대상이고, 보안 패치가 아닌 단순 버전 bump다. 06-01 정리(#157~#163)와 같은 맥락으로, 데모 안정성을 위해 불필요/위험한 메이저 업데이트를 정리한다.

## 2. 평가

유지(안전):
- #320 — pdfbox 3.0.3 → 3.0.7 (패치). 위험 낮음.
- #321 — archunit-junit5 1.3.0 → 1.4.2 (마이너, 테스트 전용). 위험 낮음.

닫음(메이저/브레이킹):
- #322 — spring-dotenv 4 → 5 (메이저). 환경설정 로딩 breaking 위험.
- #323 — build_runner·riverpod 묶음. riverpod 메이저는 breaking → 보류.
- #324 — kakao_flutter_sdk_user 1 → 2 (메이저). 로그인 SDK breaking → 의도적 마이그레이션 필요.
- #325 — flutter_secure_storage 9 → 10 (메이저). 토큰 저장소 breaking 위험.
- #326 — just_audio 0.9.46 → 0.10.5 (0.x 관행상 breaking 가능).
- #327 — google_fonts 6 → 8 (메이저 2단계, 꾸미기성). 시연 불필요.

- 8건 모두 `master` 대상이라 dev 플로우와 불일치.

→ 결론: 안전한 패치/마이너 2건(#320·#321)만 유지, 메이저/브레이킹 6건(#322~#327) 닫음.

## 3. 절차

1. `gh pr list`로 #320~#327 메타 확인(작성자=dependabot, base=master, 버전 변화).
2. 닫을 6건에 보류 사유 코멘트 동시 등록.
3. 6건 `gh pr close` 실행.
4. `gh pr view`로 state=CLOSED 재검증(코멘트 등록 포함).
5. (권장) `.github/dependabot.yml`을 시연까지 일시중지 또는 security-only로 설정해 재제안 방지. 메이저 업그레이드는 시연 후 의도적으로 진행.

## 4. 검증 명령

```powershell
gh pr list --repo Tae0072/QT-AI-2nd-Team-Project --state open --json number,baseRefName,title
gh pr close <n> --repo Tae0072/QT-AI-2nd-Team-Project --comment "..."
gh pr view <n> --repo Tae0072/QT-AI-2nd-Team-Project --json number,state,closed
```
