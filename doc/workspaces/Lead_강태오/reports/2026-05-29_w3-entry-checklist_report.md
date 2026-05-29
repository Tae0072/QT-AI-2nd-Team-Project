# W3 진입 체크리스트 (Feature Freeze 준비)

- 작업자: Lead 강태오
- 날짜: 2026-05-29 (W2 마지막 날)
- 다음 주차: W3 2026-06-01 ~ 06-05 — Feature Freeze, 관리자·통합 조정, 경계 위반 차단
- 기준: `00_개발_일정_총괄표.md`, 2026-05-28 W2 마감 통합 점검 리포트

## 1. 빌드/머지 상태

| 항목 | 상태 |
|------|------|
| dev 빌드 | BUILD SUCCESSFUL (전체 회귀 통과) |
| 열린 PR | #146 mission 진행률 배치, #147 report 대상 검증 (둘 다 Lead, 리뷰 대기) |

## 2. 2026-05-29 신규 머지 (dev)

| PR | 내용 |
|----|------|
| #140 | report 신고 접수 API (POST /api/v1/reports) |
| #141 | mission 진행률 읽기 모델 (컨트롤러 없는 2테이블) |
| #142 | mission 리포지토리 슬라이스 테스트 + HIDDEN 노출 제외 |
| #143 | 대시보드 missionProgress 위젯 연결 |
| #145 | **reports/mission/admin 누락 Flyway 마이그레이션 보강(기동 차단 hotfix)** |
| (타) | #144 DeepSeek 클라이언트(강상민), #137 카카오 로그인 Flutter |

## 3. Feature Freeze 진입 — 도메인별 상태

| 도메인 | 상태 | 비고 |
|--------|------|------|
| member/auth | 완성 | 로그인·프로필·닉네임·탈퇴·대시보드 |
| bible | 완성 | 성경 조회 |
| qt | 완성 | Today QT + 노트 연동 |
| note | 완성 | CRUD + 묵상 이벤트·달력 |
| ai | 대부분 완성 | 검증 체크리스트·참조·DeepSeek 호출. 사용자 Q&A 경로 점검 필요 |
| praise | 완성 | 큐레이션 |
| report | 완성 | 접수 + 대상 검증(#147 머지 시). COMMENT/AI_ASSET 검증은 후속 |
| mission | 기능 완성 | 읽기 모델 + 대시보드 + 진행률 배치(#146 머지 시). MONTHLY만, enroll 트리거 후속 |
| admin | 권한 체계 구현 | 세부 권한·감사 로그 |
| audit | 기본 | |
| sharing | 부분 | 피드 조회 구현, **CommentUseCase 미구현** |
| study | 부분 | VerseExplanation·학습 콘텐츠 일부 |
| notification | 부분 | |

## 4. W3 진입 전 블로커 / 리스크

### 높음
1. **스키마-엔티티 드리프트 CI 미검출(구조적):** test 프로파일이 `flyway off + create-drop`이라 "엔티티 추가, 마이그레이션 누락"을 CI가 못 잡는다. #145로 5개 테이블은 보강했으나 근본 가드가 없다. → **Testcontainers MySQL로 Flyway migrate + ddl-auto=validate 컨텍스트 로드 테스트를 CI에 추가** 권장.
2. **dev 프로파일(H2+Flyway) 기동 불가:** 기존 V13이 H2 비호환 구문(다중 ADD COLUMN). V13은 적용 완료라 수정 불가. → dev를 MySQL로 두거나 로컬 H2는 flyway off+create-drop로 분리 결정 필요.
3. **#146/#147 머지 마무리:** Lead 기능 2건 리뷰·머지.

### 중간
4. **sharing CommentUseCase 미구현** — 댓글 기능 + report COMMENT 대상 검증 차단(이승욱 조율).
5. **mission 진행률 enroll 트리거** — 신규 회원 최초 진행 레코드 생성 경로(노트 저장 이벤트 또는 가입 시).
6. **mission DAILY/WEEKLY** — note 기간별 집계 api 필요.

### 낮음
7. notification/study 잔여 기능. AI_ASSET 사용자 조회 api(report AI_ASSET 검증용).

## 5. W3 권장 액션 (우선순위)

1. #146/#147 머지 → mission/report 기능 닫기.
2. 스키마 드리프트 CI 가드 도입(가장 비용 대비 효과 큼) + admin 테이블(#145) 담당(김지민) 공유.
3. dev 프로파일 DB 전략 결정(H2 vs MySQL).
4. Feature Freeze 경계: 신규 기능 제한, sharing 댓글/ mission enroll 등 잔여는 담당자별 마무리 또는 v1.1 이월 판정.
5. 도메인 간 통합 테스트(qt↔note↔mission↔dashboard, report↔sharing/ai) 점검.

## 6. 판정

**W3 진입: 조건부 PASS.** 핵심 사용자 흐름(로그인→QT→노트→나눔/신고→대시보드/미션) 데모 가능. 단 (1) 스키마 드리프트 가드와 (2) dev 기동 환경은 W3 초반에 정리해야 운영 빌드가 안정화된다.
