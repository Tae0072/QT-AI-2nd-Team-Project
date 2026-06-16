# 워크플로우 — README 프로젝트 완료 정리 (변경 이력 + 완료 현황 추가)

- 작성자: Lead 강태오
- 날짜: 2026-06-16
- 대상 저장소: 문서 저장소(2nd-Team-Project), 구현 저장소(QT-AI-2nd-Team-Project)
- 관련 F-ID: 전체(F-01~F-16) 기준 — 문서/구현 메타 정리(코드 동작 변경 없음)

## 1. 목적

프로젝트 종료에 맞춰 두 저장소 README를 갱신한다. 기존 본문은 그대로 두고(덮어쓰기 금지),
끝에 "변경 이력(진행 순서)"과 "프로젝트 완료 현황" 섹션만 순서대로 추가한다.

## 2. 작업 순서

1. 두 저장소의 `master`/`dev` 브랜치 README와 최종 구조를 확인.
   - 문서 저장소 README 기준: `origin/master`(151줄).
   - 구현 저장소 README 기준: `origin/dev`(224줄, MSA 실행 가이드 포함).
2. 최종 구현 상태 조사: `qtai-server` 멀티모듈 6개(lib-common + 4 service + admin-server) + nginx 게이트웨이, 도메인 16종, `admin-web`, `flutter-app`, `k8s/`.
3. 기준 문서 최신 버전 확인: 요구사항 v3.5, 아키텍처 v1.3, API v1.7, ERD v2.2.
4. 각 README 끝에 추가할 섹션 작성(문서용/구현용 분리).
5. 기존 본문 보존 검증 + 금지 패턴 자체 점검 후 PR 초안 작성.

## 3. 변경 범위

- 문서 저장소: `README.md` 끝에 "변경 이력 / 프로젝트 완료 현황" 추가.
- 구현 저장소: `README.md` 끝에 "변경 이력 / 프로젝트 완료 현황" 추가 + 본 워크플로우/리포트 문서 추가.
- 코드·스키마·API 변경 없음(문서 전용 PR).

## 4. 검증

- 기존 README 본문 라인 보존(문서 151줄, 구현 224줄) 위에 append만 수행.
- Requirements Guard 금지 패턴은 모두 부정/제외 문맥에서만 사용(`개역개정 금지`, `Kafka는 AI 영역 한정`, `로컬 한정 K8s` 등).
- 상세 결과는 동일 날짜 리포트(`reports/2026-06-16_readme-project-complete_report.md`) 참조.
