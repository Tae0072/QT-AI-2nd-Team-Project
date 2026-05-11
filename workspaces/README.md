# QT-AI Workspaces

> 각 팀원이 자신의 **개인 폴더에서만** 읽기·쓰기가 허용됩니다.

## 폴더 구조

```
workspaces/
├── README.md             # 이 파일
├── _template.md          # 워크플로우/리포트 공통 템플릿 (수정 금지)
├── Lead_강태오/
│   ├── workflows/            # 작업 시작 전 반드시 작성
│   └── reports/              # 작업 완료 후 반드시 작성
├── DevA_이지윤/  (동일 구조)
├── DevB_김태혁/  (동일 구조)
├── DevC_강상민/  (동일 구조)
├── DevD_이승욱/  (동일 구조)
└── DevE_김지민/  (동일 구조)
```

## 필수 규칙

### 1. 타인 폴더 접근 금지

- 다른 팀원의 `workspaces/` 폴더는 읽기·수정·삭제 모두 금지
- AI 에이전트(Claude Code, Cursor 등)도 동일하게 적용

### 2. 프로젝트 영향 금지

- `workspaces/` 내부 파일은 빌드·런타임·테스트·CI에 **영향 0**
- 다른 서비스 코드에서 import 대상이 아님

### 3. 워크플로우 → 리포트 의무

```
1. 작업 시작 전
   → workflows/YYYY-MM-DD-{task-name}.md 작성

2. 작업 수행

3. 작업 완료 직후 (다음 작업 전에 반드시)
   → reports/YYYY-MM-DD-{task-name}.md 작성
```

- 1 워크플로우 = 1 리포트 (동일 파일명)

### 4. PR 교차 금지

- 다른 팀원 폴더 변경이 PR에 포함되면 자동 reject

### 5. 텞플릿 수정 금지

- `_template.md`는 공통 양식 — 수정 시 Lead에게 제안

## 파일명 규칙

```
workflows/YYYY-MM-DD-{kebab-case-task}.md
reports/YYYY-MM-DD-{kebab-case-task}.md

예시:
  workflows/2026-05-12-auth-jwt-filter.md
  reports/2026-05-12-auth-jwt-filter.md
```
