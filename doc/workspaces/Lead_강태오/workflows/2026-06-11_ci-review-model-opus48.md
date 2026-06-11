# 워크플로우 — PR 자동 리뷰 모델 변경 (Opus 4.8)

- 날짜: 2026-06-11
- 브랜치: chore/ci-review-model-opus48
- PR: #528 (base: dev)
- 변경 파일: .github/workflows/claude-pr-review.yml

## 목표
GitHub PR 자동 리뷰 워크플로우가 사용하는 Claude 모델을 claude-fable-5 → claude-opus-4-8로 변경한다.

## 작업 단계
1. origin/dev 최신화(fetch) 후 격리된 git worktree로 작업 브랜치 생성 (메인 dev 작업트리에 다른 작업 스테이징이 많아 영향 차단).
2. .github/workflows/claude-pr-review.yml env 블록 수정: ANTHROPIC_MODEL_NAME claude-fable-5 → claude-opus-4-8, 설명/예시 주석 동기화.
3. git diff로 3줄 변경만 있는지 2~3회 검토.
4. Conventional Commit 커밋 → origin push → gh로 dev 대상 PR(#528) 생성.

## 트러블슈팅
- 리눅스 샌드박스 git이 .git 쓰기 실패(Operation not permitted)로 .git/config가 널바이트로 손상됨. Windows 네이티브 git으로 config 복구 후 모든 git 작업을 Windows에서 수행.