# 워크플로우 — dev → master 승격(릴리스) + 스쿼시 이력 단절 정렬

- 작업자: Lead 강태오
- 날짜: 2026-06-01
- 대상 저장소: QT-AI-2nd-Team-Project
- 기준 문서: `09_Git_규칙.md`, `CLAUDE.md` §12
- PR: #173 (머지됨)

## 1. 배경

dev의 누적 작업(109 커밋)을 master로 승격해야 한다. 점검 결과 dev→master 직접 병합 시 약 140개 충돌이 발생했다.

## 2. 원인 분석

직전 dev→master 병합(#61, 5/20)이 **스쿼시 병합**으로 처리되어 공통 조상이 끊겼다. 그 결과 git이 master/dev를 거의 남남으로 보고 대부분 파일에 가짜 충돌(add/add, modify/delete)을 표시한다. 실제 코드 충돌이 아니라 이력 단절의 착시이며 dev가 최신·정답.

## 3. 절차

1. `git merge-tree`로 충돌이 스쿼시 후유증임을 확인(merge-base가 5/20로 매우 오래됨, #61이 단일 부모=스쿼시).
2. dev 빌드/회귀 정상 확인.
3. `chore/release-dev-to-master` 브랜치에서 `git merge -s ours origin/master` — master를 부모로 기록하되 트리는 dev와 동일하게 정렬(수동 충돌 해결 0).
4. 검증: 트리 == dev(`git diff origin/dev` 비어 있음), 부모 = [dev, master](이력 복원).
5. master 대상 PR 생성. 머지 방식은 **Merge commit**(스쿼시 금지 — 재발 방지) 권장.

## 4. 정책 준수

- master 직접 push 금지 — PR로 진행.
- 효과: 이후 dev→master 병합은 충돌 없이 진행.

## 5. 검증 명령

```powershell
git merge-tree --write-tree origin/master origin/dev   # 충돌 진단
git merge -s ours origin/master                         # 정렬
git diff origin/dev                                     # 트리 동일(빈 출력) 확인
```
