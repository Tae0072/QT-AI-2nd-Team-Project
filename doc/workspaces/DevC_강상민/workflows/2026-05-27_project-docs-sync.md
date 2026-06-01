# Workflow — 2026-05-27 project-docs-sync

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `docs/project-docs-sync` |
| PR 대상 | `dev` |
| 변경 유형 | docs |
| 원본 저장소 | `https://github.com/Tae0072/2nd-Team-Project.git` |
| 원본 브랜치 | `master` |
| 원본 커밋 | `7f6ac87f9fbd7e2d4109e752dbcf2daf98378ca` |
| 대상 경로 | `doc/프로젝트 문서/` |

## 작업 목표

외부 문서 저장소의 최신 기준 문서를 현재 프로젝트 기준 문서 경로에 동기화한다.
단순 전체 `doc/` 덮어쓰기는 저장소 구조 차이 때문에 제외하고, 동일 파일명 기준의 프로젝트 기준 문서만 반영한다.

## 기준 문서

- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/05_시퀀스_다이어그램.md`
- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/09_Git_규칙.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/22_구현_저장소_반영_체크리스트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `doc/프로젝트 문서/25_기능_명세서.md`

## 작업 범위

- 외부 `master`의 기준 문서 9개를 파일명 기준으로 현재 프로젝트 문서 경로에 반영한다.
- `03_아키텍처_정의서.md`는 v1.3 기준으로 v1.1/v1.2 패키지 구조와 관리자 웹 분리 이력이 유지되는지 확인한다.
- `04_API_명세서.md`는 제목/메타데이터 버전 정합성과 민감값 형태 예시를 확인한다.
- 동기화 결과 보고서를 `doc/workspaces/DevC_강상민/reports/`에 남긴다.

## 제외 범위

- 외부 저장소 루트에만 있고 현재 프로젝트에 대응 파일이 없는 문서 추가는 제외한다.
- 서버 코드, OpenAPI YAML, 테스트 코드는 변경하지 않는다.
- 외부 저장소의 build output, `.gradle`, PDF, 성경 JSON, 주석 PDF 등은 가져오지 않는다.

## 구현 순서

1. 현재 브랜치와 작업트리 상태를 확인한다.
2. 외부 저장소 `master`를 `external-docs/master`로 fetch한다.
3. 기준 문서 9개를 파일명 기준으로 `doc/프로젝트 문서/`에 복사한다.
4. 리뷰에서 지적된 문서 회귀 항목이 최신 외부 문서에 반영됐는지 확인한다.
5. 민감값 형태 placeholder가 남아 있으면 `<redacted>`로 로컬 보정한다.
6. 동기화 리포트를 DevC 담당자 경로에 기록한다.
7. `git diff --check`와 민감값 패턴 검색을 실행한다.

## 검증 기준

- [ ] `03_아키텍처_정의서.md`에 `api/internal/web/client` 구조와 §3.1.1, §9.6이 유지된다.
- [ ] `04_API_명세서.md`의 제목 버전과 문서 버전이 일치한다.
- [ ] 금지 번역본 seed/response/test 데이터가 추가되지 않는다.
- [ ] plain secret, token, password, private key 예시가 추가되지 않는다.
- [ ] `git diff --check`가 통과한다.
- [ ] 민감값 형태 예시 패턴 검색이 통과한다.

## 산출물

- `doc/workspaces/DevC_강상민/workflows/2026-05-27_project-docs-sync.md`
- `doc/workspaces/DevC_강상민/reports/2026-05-27_project-docs-sync_report.md`
