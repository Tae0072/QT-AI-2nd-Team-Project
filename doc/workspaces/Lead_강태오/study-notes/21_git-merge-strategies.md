# Git 고급 병합 — squash 후유증과 `merge -s ours`

> **왜 배워야 하나:** 노션에서 git 기초(merge, rebase, reset)는 배웠지만, "스쿼시 병합이 나중에 충돌 지옥을 만드는 이유"와 "그걸 푸는 `-s ours` 전략"은 다루지 않았다. 이번 dev→master 릴리스에서 약 140개의 가짜 충돌이 나서 이걸로 해결했다.

---

## 1. 병합 방식 3가지 복습

| 방식 | 결과 | 특징 |
|---|---|---|
| Merge commit | 두 브랜치를 잇는 병합 커밋 생성 | 양쪽 이력(부모 2개) 보존 |
| Squash merge | 여러 커밋을 **1개로 합쳐서** 올림 | 이력 깔끔, 단 **연결고리(부모)가 끊김** |
| Rebase | 커밋을 다른 베이스 위로 옮겨 붙임 | 일직선 이력 |

## 2. 이번 문제 — 스쿼시 후유증

상황: `dev`를 `master`에 병합하려는데 **약 140개 파일에서 충돌**(add/add, modify/delete).

원인 분석:
- 직전 dev→master 병합이 **스쿼시**로 처리됐다.
- 스쿼시는 dev의 여러 커밋을 1개로 합쳐 master에 올리는데, 이때 **"master와 dev가 같은 뿌리에서 나왔다"는 연결고리(공통 조상)가 끊긴다.**
- 그래서 git이 두 브랜치를 거의 남남으로 보고, 같은 파일도 "양쪽에서 새로 만든 것"으로 취급 → 가짜 충돌이 무더기로 뜬다.
- **실제 코드 충돌이 아니라 이력 단절의 착시.** master는 옛 스냅샷, dev가 최신·정답.

진단 명령:
```bash
git merge-tree --write-tree origin/master origin/dev   # 충돌 미리보기(실제 병합 안 함)
git show -s --format="%p" <머지커밋>                    # 부모 1개면 스쿼시였다는 신호
git merge-base origin/master origin/dev                 # 공통 조상이 너무 옛날이면 단절 의심
```

## 3. 해결 — `git merge -s ours`

"master를 부모로 **기록만** 하고, 실제 내용(트리)은 dev 것을 그대로 쓴다"는 전략:

```bash
git checkout -b release/dev-to-master dev
git merge -s ours origin/master -m "release: master를 dev에 정렬"
# 결과: 트리 = dev와 100% 동일, 부모 = [dev, master]  → 끊긴 이력 연결 복원
```

검증:
```bash
git diff origin/dev      # 비어 있어야 함(트리가 dev와 동일)
git show -s --format="%p" HEAD   # 부모 2개(dev, master) 확인
```

> 주의: `-s ours`(전략)와 `-X ours`(옵션)는 다르다.
> - `-s ours`(strategy): 상대 내용을 **통째로 무시**하고 내 트리만 남김.
> - `-X ours`(option): 충돌난 부분만 내 쪽으로 자동 선택(나머지는 정상 병합).

## 4. 재발 방지

- master로 올릴 때 **Merge commit 방식**을 쓴다(스쿼시 금지). 스쿼시로 또 합치면 같은 단절이 반복된다.
- 평소 PR(기능→dev)은 스쿼시를 써도 무방하지만, **장수 브랜치(dev↔master) 사이 병합은 merge commit**이 안전.

## 5. 일상 팁 (충돌 줄이기)

PR 올리기 전 항상 dev 최신을 내 브랜치에 먼저 병합:
```bash
git checkout dev && git pull origin dev
git checkout feature-branch && git merge dev   # 여기서 충돌 미리 해결
```

## 6. QT-AI에서의 적용

- dev→master 릴리스(#173)에서 스쿼시 후유증 140개 충돌을 `merge -s ours`로 정렬, 이력 복원.
- 이후 dev→master 병합은 충돌 없이 진행되도록 머지 방식을 merge commit으로 통일.

## 7. 참고 자료

- git merge 전략: https://git-scm.com/docs/git-merge#_merge_strategies
- merge-tree: https://git-scm.com/docs/git-merge-tree
