# bible-service Inc3 소비자 HTTP 어댑터 설계 (MSA Phase 1) — 2026-06-09

> Inc2(라우트 컷오버)까지 **외부 HTTP 경로**는 bible-service로 전환됨. Inc3는 **모놀리식 내부 소비자**가
> bible를 in-process 대신 HTTP로 호출하도록 전환한다.
> 작성: DevD 이승욱 (Lead 강태오 계정) · **협의 필요: bible 오너(이지윤)·qt/study 오너(김태혁)·Lead** (아키텍처 결정 사안)

## 1. 현황 — 모놀리식 내부 bible 소비자
모두 bible의 공개 포트(api/UseCase)를 in-process 주입해 호출(경계는 이미 깔끔):
- `qt`: `BibleBookLookup`(ListBibleBooks), `QtTodayPassageImportService`, `TodayQtRangeMapper`
- `note`: `NoteService`
- `study`: `StudyService`
- `ai`: `ExplanationGenerationJobHandler` — **ai는 ai-service로 별도 추출 중**(강상민), bible client는 ai-service가 자체 보유 → 본 트랙 제외 가능
- 포트: `ListBibleBooksUseCase.listBibleBooks()`, `GetBibleVerseUseCase.getVerse(Long)` / `getVerses(bookCode, chapter, from, to)`

## 2. 핵심 결정 사항 (협의 필요)

### (A) 빈 선택 — in-process vs HTTP
- bible 도메인의 `BibleService`(@Service)가 api/UseCase를 in-process 구현. HTTP 어댑터는 **동일 인터페이스의 대체 구현** → 빈 충돌.
- **제안**: `qtai.bible.client.mode = inprocess(기본) | http`. `http`일 때만 HTTP 어댑터를 `@Primary`로 등록(ai-service `client.mode=mock|http` 패턴 동일). 기본 inprocess라 **동작 무변경**, 플래그로 전환·롤백.
- 위치: 여러 도메인이 공유하는 외부 호출 → `external/bible`(CLAUDE.md §4: 공유 외부 시스템은 external/).

### (B) 서비스-to-서비스 인증 (⚠ 보안 모델 변경)
- 소비 지점 다수가 **배치/캐시 경계 — 사용자 컨텍스트 없음**(예: Today QT 캐시 갱신·일일 수집). 그런데 bible-service 필터는 deny-by-default로 `X-Member-Id` 요구.
- **제안**: bible-service `GatewayHeaderAuthenticationFilter`에 **SYSTEM 주체** 허용 추가 — 유효한 `X-Gateway-Token`이 있으면 사용자 헤더 없이도 통과(서비스 호출). 토큰 미설정(dev)에서는 기존처럼 `X-Member-Id` 요구.
  - 근거: bible는 읽기 전용 참조 데이터라 member 단위 인가가 없고, 신뢰 경계는 "게이트웨이/내부 서비스만 호출"이며 그 증명이 공유 토큰. 사용자 호출은 게이트웨이가 토큰+신원헤더를 함께 주입.
  - SYSTEM 호출은 `SYSTEM_BATCH` 주체로 로깅(CLAUDE.md §5).
- **대안**: 모놀리식 HTTP 클라이언트가 고정 SYSTEM member id 헤더를 주입(필터 무변경). 단 "가짜 사용자" 신원이 audit를 오염시킬 수 있어 SYSTEM 주체 방식 선호.
- 이 변경은 Inc2에서 리뷰받은 필터 의미를 바꾸므로 **Lead/리뷰 합의 필요**.

### (C) 호출 경로 — 게이트웨이 경유 vs 직접
- **제안**: 내부 서비스 호출은 게이트웨이 미경유 **직접 호출**(bible-service URI) + 공유 토큰. 게이트웨이는 외부 사용자 트래픽 전용.

## 3. 증분 분할 (소비자별·저위험)
| Inc | 범위 | 비고 |
|-----|------|------|
| **3a** | `external/bible` HTTP 클라이언트 + api 어댑터(`@Primary`, mode=http 게이트) + bible-service SYSTEM 주체 필터 + 계약/단위 테스트 | **동작 무변경(기본 inprocess)**. 인프라+보안 토대 |
| **3b** | `qt` 소비자 전환 검증(mode=http에서 qt 경로 계약 테스트) | 오너 김태혁 협의 |
| **3c** | `note` 소비자 전환 검증 | |
| **3d** | `study` 소비자 전환 검증 | 오너 김태혁 협의 |

각 단계는 인터페이스 불변(소비자 코드 무변경) — 빈 선택만 바뀐다. 계약 테스트(WireMock 소비자 + bible-service 컨트롤러 provider)로 보호.

## 4. 위험 / 협의
- **(B) 보안 모델 변경**이 가장 중요 — Lead/리뷰 합의 없이 진행 불가.
- 타 도메인(qt/study=김태혁, note) 소유 — 빈 선택만 바뀌고 코드는 불변이나 오너 통지 필요.
- 계약 드리프트: bible-service 응답 스키마 변경 시 소비자 깨짐 → 계약 테스트 필수.
- ai는 ai-service 트랙에서 자체 bible client 보유 → 본 트랙 제외.

## 5. 권장 첫 슬라이스 (Inc3a)
동작 무변경(기본 inprocess)으로 ① `external/bible` HTTP 클라이언트 + 어댑터(gated) ② bible-service SYSTEM 주체 필터 + 테스트 ③ 계약 테스트. 소비자 전환(3b~3d)은 오너 협의 후.

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업) · **(B) 보안 모델은 Lead 합의 선행**
