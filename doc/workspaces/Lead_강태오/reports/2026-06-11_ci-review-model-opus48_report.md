# 리포트 — PR 자동 리뷰 모델 변경 (Opus 4.8)

- 날짜: 2026-06-11
- 브랜치: chore/ci-review-model-opus48
- PR: #528 → base dev
- 변경 파일: .github/workflows/claude-pr-review.yml (1 file, +3 / -3)

## 변경 요약
PR 자동 리뷰가 사용하는 모델을 claude-fable-5에서 claude-opus-4-8로 교체. 관련 주석도 일치하도록 갱신.

## 변경 상세
- 주석: 코드 리뷰는 Fable(최상위 추론력) 사용 → 코드 리뷰는 Opus 4.8 사용
- 주석: 예시 (claude-opus-4-8 / claude-sonnet-4-6 다운그레이드) → (claude-fable-5 / claude-sonnet-4-6 변경 가능)
- 값: ANTHROPIC_MODEL_NAME: claude-fable-5 → claude-opus-4-8

## 검토
- 브랜치명 chore/... → pr-validation Branch Name Convention 정규식 통과 확인
- BOM 없음, 탭 미혼입, 변경 3줄 한정 확인
- 워크플로우 설정 외 코드 변경 없음

## 후속
- PR #528 자동 리뷰/CI 결과 확인 후 dev 머지.