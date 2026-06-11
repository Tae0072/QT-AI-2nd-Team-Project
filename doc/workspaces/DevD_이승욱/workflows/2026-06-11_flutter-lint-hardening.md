# 2026-06-11 flutter 린트 강화 (chore/mobile-lint-hardening)

## 목표·배경
코드리뷰 TODO 3 (P2): `analysis_options.yaml`이 기본 flutter_lints만 사용 — print·BuildContext 오용·떠다니는 Future에 대한 예방 규칙 부재.

## 작업 내용
- 규칙 3종 추가: `avoid_print`, `use_build_context_synchronously`, `unawaited_futures` (각 규칙의 사유 주석 포함)
- 신규 경고 9건(전부 `unawaited_futures`) 같은 PR에서 정리:
  - music_providers 4건 — `_persist(...)` 서버 저장은 fire-and-forget 의도 → `unawaited()` 명시
  - Navigator push류 5건(login/profile_edit×2/app_router/테스트) — 화면 전환 Future는 다음 라우트 pop까지 완료되지 않아 대기 부적절 → `unawaited()` 명시
- `avoid_print`·`use_build_context_synchronously` 위반 0건(기존 코드 양호)
- CI: qt-ai-ci.yml flutter 잡이 analyze를 이미 실행 — 추가 설정 불필요 확인

## 검증
- `flutter analyze` 무이슈, `flutter test` 141건 전체 통과

## 미해결 / 후속
- 없음

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
