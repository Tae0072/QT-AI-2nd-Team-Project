# 2026-06-09 MSA Phase 1 — bible-service SYSTEM 주체 인증 (Inc3a)

## 목표
모놀리식 내부 소비자(qt/note/study)가 bible를 HTTP로 호출(Inc3)하려면, **사용자 컨텍스트 없는 배치/캐시 경로**(서비스-to-서비스)도 bible-service를 호출할 수 있어야 한다. deny-by-default 필터에 **SYSTEM 주체**를 허용해 그 토대를 만든다.

## 배경
- 설계: `bible-service-Inc3-소비자HTTP어댑터-설계_2026-06-09.md` 결정 (B). bible 소비 다수가 Today QT 캐시 갱신·일일 수집 등 배치 경계라 사용자 신원이 없다.
- **보안 모델 변경(Lead 합의 사안)**: Inc2에서 리뷰받은 필터는 `X-Member-Id` 필수였다. 토큰을 "서비스 신뢰 자격"으로 격상한다(옵션 1 채택, 2026-06-09 T 결정).

## 작업 내용
`GatewayHeaderAuthenticationFilter` 로직 변경 — 토큰을 1차 기준으로:
- **토큰 설정 환경**(`qtai.bible.gateway.shared-token`): 유효한 `X-Gateway-Token`이면 신뢰. ① 게이트웨이가 JWT 인증한 사용자 호출(신원 헤더 동반) ② 내부 SYSTEM 서비스 호출(헤더 없음) **모두 통과**. 불일치/누락 → 401. 사용자 헤더 없는 통과는 `SYSTEM_BATCH` 주체로 debug 로깅.
- **토큰 미설정(dev)**: 기존대로 `X-Member-Id`+`X-Member-Role` 요구(SYSTEM 호출은 토큰 환경에서만).
- 근거: bible는 읽기 전용 참조 데이터라 member 단위 인가가 없고, 신뢰 증명은 공유 토큰(서버 비밀, 위조 불가·상수시간 비교). 게이트웨이 미경유 직접 호출자는 토큰이 없어 401.

## 범위
- 브랜치: `feature/msa-bible-http-client` (base: `dev`)
- 변경: 필터 1파일 + 테스트(+3) + Inc3 설계 문서. 소비자(qt/note/study)·게이트웨이·코어 무변경.
- 관련: bible 추출 Inc3a — Inc3b~3d(소비자 HTTP 전환)의 인증 토대

## 검증
- `gradlew :bible-service:test` — **BUILD SUCCESSFUL / 0 failures (18건)**
  - `GatewayHeaderAuthenticationFilterTest` 9(+2: 토큰+헤더없음 SYSTEM 통과 / 토큰미설정 SYSTEM 불가 401)
  - `BibleGatewayTokenSliceTest` 4(+1: 토큰만으로 SYSTEM 호출 200) + 슬라이스 2 + persistence 1 + 캐시 1 + contextLoads 1
- **기존 테스트 전부 유지**(토큰 미설정=헤더 요구, 토큰설정+불일치=401 등 의미 불변, SYSTEM 경로만 추가).

## 리뷰 후속 보강(2차)
- **감사 트레일 격상**: SYSTEM 호출 로그를 `debug` → 전용 감사 로거(`com.qtai.audit.bible`) **INFO**로(주체=SYSTEM_BATCH, method·path만, token/PII 미기록). 감사 appender로 라우팅 가능.
- **부분 신원 헤더 테스트**: 토큰 유효+role 누락(부분 헤더) → 통과(토큰이 게이트), 토큰 미설정+id만 → 401(dev 2종 필수) 단언 추가.
- **토큰 유출 대비 정책**: 설계 문서 (D) 추가 — 서비스별 개별 토큰(범위 한정)·env 주입·정기/유출 시 회전·무중단 회전용 grace window(현재값+직전값, Inc3b 확장)·mTLS/네트워크 정책 병행.

## ⚠ 머지 상태 — Lead 합의 대기(머지 보류)
본 변경은 Inc2 승인 필터의 보안 의미를 바꾸므로, **Lead 합의 기록(이슈 코멘트/결정 문서 링크)이 PR에 첨부되기 전까지 머지 보류**한다.
- [ ] Lead 합의 기록 첨부(토큰=서비스 신뢰 자격 격상 승인)
- 합의 기록: _(PR/이슈 링크 추가 예정)_

## 미해결 / 후속
- ⚠ **Lead/리뷰 합의**: 토큰=서비스 신뢰 자격으로의 모델 변경은 Inc2 승인 필터 의미를 바꾸므로 합의 필요(위 머지 보류 체크 참조).
- **Inc3b~3d**: `external/bible` HTTP 클라이언트 + api 어댑터(`@Primary`, `qtai.bible.client.mode=http` 게이트, 기본 inprocess=무변경) → qt/note/study 소비자 전환(오너 협의) + 계약 테스트.
- Inc4(DB 분리) → Inc5(모놀리식 bible 제거).

## 담당
- DevD 이승욱 (Lead 강태오 계정으로 작업) · 보안 모델 변경은 Lead 합의 선행
