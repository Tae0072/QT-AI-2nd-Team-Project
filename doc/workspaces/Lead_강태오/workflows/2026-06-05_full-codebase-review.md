# 2026-06-05 전체 코드베이스 정밀 리뷰 워크플로우

## 목적
작업 저장소(QT-AI-2nd-Team-Project) 전체를 전 라인 정독해 ① 오류 ② 중복 ③ 충돌 ④ 수정 필요 사항 ⑤ MSA 분리 시 문제 요소를 식별한다. 읽기 전용 — 소스 수정 없음.

## 절차

1. **Git 상태 확인 (pull 검증 규칙 준수)**
   - `.git/index` 손상(bad signature) 발견 → 작업 트리 == HEAD 확인(파일 해시·629개 java 파일 수 대조) 후 `git read-tree HEAD`로 인덱스 재구성, 잔존 `index.lock` 제거.
   - `git fetch origin dev master` → 로컬 작업 트리(be88e9e)와 GitHub dev 최신(76e9b9f)의 **트리 해시 동일** 확인(#254 squash 머지분) → 최신 코드 기준 리뷰임을 보장.
   - 로컬 master가 origin/master와 7/14 어긋남 발견 → 보고서에 정리 권고 기재.

2. **구조 인벤토리**
   - 도메인 13개 + common/config/security/external/batch 파일 수·LOC 집계(서버 main 629파일 23.5k LOC, 테스트 27.6k, Flutter ~10k).
   - Flyway V1~V23 전수 정독으로 크로스 도메인 FK 맵 작성(MSA 분석 기초).

3. **정밀 리뷰 (1차 검토)** — 10개 영역 병렬 정독 패스
   - member / bible+qt / study / note / sharing+praise / ai-core / ai-web+PDF도구 / admin·audit·report·notification·mission / 아키텍처 테스트·CI·OpenAPI·ERD·compose / Flutter 전체.
   - 각 패스: 전 파일 전 라인 정독, CLAUDE.md §3~§10 규칙 대조, 심각도·파일:라인·근거·MSA 영향 형식으로 보고.

4. **교차 재검증 (2~3차 검토)**
   - 핵심 주장 17건을 grep+코드 원문으로 직접 재확인: ADMIN_ROLE_* 발급처 0건, 00:05 cron 2개·STALE_FALLBACK 경로, qt_passage_verses 쓰기 0건, native SQL 2건, 댓글 N+1·탈퇴 404, like 카운터 패턴, sourceNoteUnsharedAt 쓰기 0건, 닉네임 fallback 24자, AdminActionLog 사용 0건, RUNNING 스윕 부재, jacoco 부재·spectral 경로 오류, compose dev-bypass, 테스트 개인키 2파일+.env.example 안내, Flutter simulatorStatus 미파싱, getPassage 게이트 부재, Modulith 부재, RedisTemplate 빈 미사용.
   - 패스 간 상충 주장 해소: "ArchUnit 부재" 주장은 오판으로 폐기(`DomainBoundaryArchTest`가 ArchUnit 기반임을 import문으로 확인).

5. **MSA 분석 종합** — FK 맵 + 호출 그래프 + 트랜잭션 경계 + 이벤트 인프라 평가 → 차단 요소 10개·도메인별 분리 적합도·선행 작업 순서 도출.

6. **산출물 작성**
   - 리포트: `doc/workspaces/Lead_강태오/reports/2026-06-05_full-codebase-review_report.md`
   - 본 워크플로우: `doc/workspaces/Lead_강태오/workflows/2026-06-05_full-codebase-review.md`

## 결과 요약
- Critical 9건(관리자 API 전멸, 00:05 시딩 날짜 버그, qt_passage_verses 미작성, Today QT 해설·시뮬레이터 양방향 단절, 댓글 탈퇴 404, 노트삭제↔나눔 미연동, 신고 처리 빈 껍데기+ID 의미 오류, 미래 본문 선노출, Flutter iOS 카카오/cleartext/fallback 본문).
- High 16건, Medium/Low 다수, 중복·죽은 코드 7그룹.
- MSA: 코드 경계(긍정) vs DB·트랜잭션·이벤트(미흡) — 차단 요소 10개, 도메인별 적합도 표, P0~P3 로드맵 제시.
- 소스 코드 무수정. 커밋/PR 없음(문서 2건만 생성 — 커밋 여부는 Lead 판단).
