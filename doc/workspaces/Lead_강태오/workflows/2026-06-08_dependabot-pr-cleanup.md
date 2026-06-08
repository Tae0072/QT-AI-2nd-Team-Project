# 워크플로우 — dependabot PR 정리(#320~#327)

- 작업자: Lead 강태오
- 날짜: 2026-06-08
- 대상 저장소: QT-AI-2nd-Team-Project
- 기준: MVP 데모 안정성, `CLAUDE.md` §12 Git 규칙(PR 대상=dev), §8 금지 기술
- 대상 PR: #320~#327 (8건) → 전부 닫음

## 1. 배경

자동 의존성 봇(dependabot) PR 8건(#320~#327)이 06-08 동시에 열렸다. 전부 `master` 대상이고, 보안 패치가 아닌 단순 버전 bump다. 06-01 정리(#157~#163)와 같은 맥락으로, 데모 안정성을 위해 정리한다.

## 2. 평가

위험도:
- #320 — pdfbox 3.0.3 → 3.0.7 (패치). 위험 낮음(안전).
- #321 — archunit-junit5 1.3.0 → 1.4.2 (마이너, 테스트 전용). 위험 낮음(안전).
- #322 — spring-dotenv 4 → 5 (메이저). 환경설정 로딩 breaking 위험.
- #323 — build_runner·riverpod 묶음. riverpod 메이저는 breaking → 보류.
- #324 — kakao_flutter_sdk_user 1 → 2 (메이저). 로그인 SDK breaking → 의도적 마이그레이션 필요.
- #325 — flutter_secure_storage 9 → 10 (메이저). 토큰 저장소 breaking 위험.
- #326 — just_audio 0.9.46 → 0.10.5 (0.x 관행상 breaking 가능).
- #327 — google_fonts 6 → 8 (메이저 2단계, 꾸미기성).
- 8건 모두 base=`master`라 dev 플로우와 불일치.

결론:
- 1차 판단: 안전한 #320·#321만 유지, 메이저/브레이킹 6건(#322~#327) 닫음.
- 최종(같은 날 결정): #320·#321도 "안전함 ≠ 필요함"이고 base가 `master`라 그대로 머지 불가 → 시연 전 PR 목록 정리·dev 플로우 일원화를 위해 **8건 전부 닫음**.

## 3. 절차

1. `gh pr list`로 #320~#327 메타 확인(작성자=dependabot, base=master, 버전 변화).
2. 메이저/브레이킹 6건(#322~#327)에 보류 사유 코멘트 + `gh pr close`.
3. 이어서 안전 2건(#320·#321)도 사유 코멘트 + `gh pr close` (8건 완료).
4. `gh pr view`로 전부 state=CLOSED 재검증(코멘트 등록 포함).
5. (권장) `.github/dependabot.yml`을 시연까지 일시중지 또는 security-only로 설정해 재제안 방지. 필요한 업데이트는 `master`가 아닌 `dev` 대상으로 재반영.

## 4. 검증 명령

```powershell
gh pr list --repo Tae0072/QT-AI-2nd-Team-Project --state open --json number,baseRefName,title
gh pr close <n> --repo Tae0072/QT-AI-2nd-Team-Project --comment "..."
gh pr view <n> --repo Tae0072/QT-AI-2nd-Team-Project --json number,state,closed
```
