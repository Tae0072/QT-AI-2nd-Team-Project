# 2026-06-06 P1 (High) 일괄 수정 결과 보고

전체 코드 리뷰에서 식별된 **P1 13건 전부 수정 완료** — 12개 작업 브랜치/12개 커밋(P1-1·P1-8 동일 브랜치), 서버 전체 스위트 **828 tests / 0 failures**(현 브랜치 기준).
워크플로우(브랜치 체인·머지 순서): `workflows/2026-06-06_p1-fixes-batch.md`

> 작업 방식: 각 항목을 `dev`에서 분기한 독립 브랜치로 구현, 커밋까지 완료. **push·PR 생성은 사용자(T)가 직접 수행**한다(합의된 역할 분담). 본 문서는 머지 전 점검용 요약이다.

## 이슈별 결과

| # | 이슈 | 브랜치 | 커밋 | 핵심 변경 |
|---|------|--------|------|----------|
| P1-1 | 동시성 UNIQUE 위반이 500으로 노출 | `fix/common-exception-handlers` | f56e175 | `DataIntegrityViolationException`을 의미 있는 409(CONFLICT)로 매핑. 좋아요·노트 등 동시 생성 경쟁에서 사용자에게 500 대신 정상 충돌 응답 |
| P1-8 | 전역 예외 핸들러 공백(4xx가 500으로 누출) | `fix/common-exception-handlers` | f56e175 | 누락 핸들러 보강(검증·파싱·제약 위반 등)으로 4xx가 500으로 새지 않게 정리. P1-1과 같은 공통 레이어라 1개 브랜치로 묶음 |
| P1-2 | 좋아요·댓글 카운터 lost update | `fix/sharing-atomic-counters` | 558fee0 | read-modify-write를 원자 `UPDATE ... SET cnt = cnt ± 1` 쿼리로 전환. 동시 좋아요/댓글에서 카운터 유실 방지 |
| P1-3 | AI 워커 RUNNING 고착·prompt 버전 부재 | `fix/ai-worker-stabilization` | f15fe5f | 장시간 RUNNING 잡 스윕(타임아웃 → FAILED 재시도 후보)으로 큐 정지 방지, prompt 버전 시드 보강 |
| P1-4 | 임시 닉네임 fallback이 VARCHAR(20) 초과 | `fix/member-temp-nickname-length` | 5ba9796 | 충돌 시 임시 닉네임 생성 규칙이 20자를 넘겨 가입 자체가 실패하던 결함 수정(길이 보장) |
| P1-5 | 평문 테스트 RSA 키 커밋 | `chore/test-keys-dynamic` | 7ba8ed0 | 저장소에 박혀 있던 평문 테스트 키 제거 + 런타임 동적 생성으로 전환, `.env.example` 안내 정정(CLAUDE.md §8 준수) |
| P1-6 | 시연 compose 무인증 노출 | `fix/demo-compose-auth` | b4ce4ff | 시연 스택의 인증 우회 제거, 정식 JWT 인증을 켠 `demo` 프로파일 도입(시연도 실제 인증 경로 사용) |
| P1-7 | Security 구성 갭(CORS·프로파일·H2·system) | `fix/security-config-gaps` | 9910923 | CORS 정책·프로파일별 필터 갭·H2 콘솔 노출·시스템 API 보호를 정리. `SYSTEM_BATCH` 주체 경로 보호 보강 |
| P1-9 | 미션·달력 집계 결함(streak·전월·snapshot) | `fix/mission-streak-aggregation` | 402abe1 | streak을 "오늘 미저장 시 어제 앵커"로 계산하고 윈도 조회로 월 경계 연속 인정(04:00 배치 0 방지). 매월 1일 전월 마감 재계산, target 변경 시 snapshot/rate 정합(F-13) |
| P1-10 | journal_events 아웃박스 미완·재처리기 부재 | `fix/journal-events-outbox` | 43cd3e0 | `AFTER_COMMIT+REQUIRES_NEW`(유실 창)를 노트와 같은 tx의 트랜잭션 아웃박스로 전환, `@Scheduled` 재처리기(PENDING 드레인 + 지수 백오프 재시도 + dead-letter)와 `previous_qt_passage_id`(증분 집계 선반영) 추가(F-03) |
| P1-11 | study 승인·숨김 경로 비대칭 | `feature/study-simulator-glossary-publish` | ba5da06 | SIMULATOR/GLOSSARY의 게시·숨김 경로 신설(해설만 있던 publish/hide를 대칭화 — 긴급 차단 가능) |
| P1-12 | Flutter 인증·계약 잔결함 | `fix/flutter-auth-hardening` | bddba1e | `/auth/**` 401에도 refresh+전역 로그아웃 돌던 루프 차단(로그인 실패 안내 보존), 2xx success=false 응답의 Null cast 노출 방지 |
| P1-13 | 좋아요·댓글 알림 미연동 | `feature/notification-wiring` | b42937a | 좋아요·댓글 발생 시 알림 연동 + 수신자/알림 설정 검증(본인 알림 제외, 설정 OFF 존중). sharing 변경과 겹쳐 P1-2 위에 스택 |

## 검증

- 서버 전체 스위트 **828 tests / 0 failures / 4 skipped**(현 브랜치 `fix/journal-events-outbox`). skipped는 Docker 필요한 `MysqlMigrationValidationTest` 등 — 로컬 자동 skip, CI(Docker)에서 가드.
- 각 브랜치 시점에도 전체 스위트를 반복 실행해 0 failures 확인(브랜치 전환 시 origin/dev 기준으로 표시되는 건 정상 — 커밋은 각 브랜치에 보존).
- 회귀 방지 테스트를 항목마다 신규/강화. 특히:
  - P1-9: 어제앵커·월경계(29일 연속)·target 변경 snapshot·전월(1일) 마감 케이스 추가.
  - P1-10: 재처리기 상태전이(PENDING→PROCESSED)·지수 백오프·이벤트별 격리·dead-letter 보존, due 조회 규칙(PENDING/FAILED-due만), 아웃박스 in-tx 적재·`previousQtPassageId` 라운드트립.
- 비밀 점검: 본 배치는 평문 secret/key를 추가하지 않음(P1-5는 오히려 평문 키 제거). 신규 파일은 Java/SQL 코드뿐.

## 품질 게이트 메모(머지 전 확인)

- P1-10은 아웃박스 원자화 리팩터링 특성상 **16 files / +546·-226**으로 PR 권장치(10 files·500 changed lines)를 상회. 삭제 226줄의 대부분(208줄)이 구버전 핸들러 3종 제거분이라 실질 신규 면적은 작다. 분할 시 "핸들러 제거 후 재처리기 추가 전" 깨진 중간 상태가 생겨 단일 PR로 유지함 — 리뷰어에게 사전 공유 권장.
- 나머지 항목은 대체로 10 files·500 lines 내.

## 사용자/운영 영향 요약(머지 후)

- 동시 좋아요·댓글·노트 생성에서 카운터 유실·500 노출 해소(P1-1·P1-2·P1-8).
- AI 생성 큐가 RUNNING 고착 없이 진행(P1-3), 미션 streak/진행률이 04:00 배치에서 정상 집계(P1-9).
- 묵상 노트 변경 기록이 유실 창 없이 원자적으로 남고 실패해도 재처리로 복구(P1-10).
- 닉네임 충돌이 있어도 가입 가능(P1-4), 시연·보안 구성 노출 경로 제거(P1-5·P1-6·P1-7).
- 시뮬레이터/용어 콘텐츠를 긴급 숨김 가능(P1-11), 앱 로그인 실패 안내 보존·널 노출 제거(P1-12), 좋아요·댓글 알림 도착(P1-13).

## 남은 사항 / 후속

- **머지·PR 생성은 사용자(T)가 직접 수행.** 머지 순서는 워크플로우 문서의 체인을 따를 것 — 특히 **P1-2(`fix/sharing-atomic-counters`) → P1-13(`feature/notification-wiring`)** 순서 준수(스택 의존).
- 전 브랜치 머지 후 `dev`에서 전체 테스트 1회 + (Docker 환경이면) `MysqlMigrationValidationTest` 포함 실행 권장.
- P1-10 증분 집계 소비자(`JournalEventDeliveryHandler` 구현)는 후속 범위 — 현재는 라이브 읽기 경로가 집계, 스키마(`previous_qt_passage_id`)만 선반영.
- P2/P3는 조치 계획서대로 미착수.
