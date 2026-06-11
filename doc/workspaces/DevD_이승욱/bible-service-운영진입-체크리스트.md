# bible-service 운영 진입(cutover) 체크리스트 — 2026-06-09~

> bible 추출(Phase 1)의 실트래픽 운영 진입 전 충족해야 할 토대 항목 트래킹.
> 담당: DevD 이승욱 (Lead 강태오 계정) · ai-service cutover readiness 체크리스트(#375)와 동일 성격.

## 1. 보안 토대
- [x] **Lead 합의** — 토큰=서비스 신뢰 자격 격상(SYSTEM 주체) 승인(2026-06-09). [[2026-06-09_msa-bible-system-principal]]
- [x] **부분 신원 헤더 다운스트림 영향 점검** — bible-service 도메인 코드는 신원 헤더·role 인가 미사용(`@PreAuthorize`/`hasRole`/`SecurityContext`/`X-Member-*` 0건, 컨트롤러는 `@RequestParam`만). **읽기 전용이라 부분/무 신원 헤더의 침묵 인가 우회 위험 없음**(우회할 인가 부재).
  - ⚠ **불변식(향후)**: bible-service에 member 단위 인가·쓰기 엔드포인트가 추가되면 SYSTEM 토큰 통과(사용자 헤더 없이 허용)를 **반드시 재검토**한다(SYSTEM 호출에 member 인가가 적용되지 않으므로).
- [x] **토큰 회전 grace window 구현** — 필터가 현재값+직전값(`qtai.bible.gateway.previous-token`)을 동시 허용(상수시간 비교) → 무중단 회전 가능. 회전 절차: 새 토큰을 current로, 기존을 previous로 설정 → 게이트웨이/소비자 전환 → previous 비움. (Inc3b 구현, [[2026-06-09_msa-bible-token-rotation]])
  - ⚠ **grace window 운영 위험(문서 보강, 차단 아님)**:
    1. **장기 방치**: previous-token을 회전 후 안 비우면 구토큰이 무기한 유효(공격 표면 잔존). → 회전 완료 즉시 비우고, **잔존 모니터링/알람**(previous 설정 후 N시간 경과 시 경고) 운영 룰 필요.
    2. **길이 불일치 사이드채널**: `MessageDigest.isEqual`은 내용 상수시간이나 **길이가 다르면 조기 false** → 토큰 길이 노출 가능. 운영상 **현재·직전 토큰 길이를 동일 규격(고정 길이 랜덤)** 으로 발급해 완화. (영향 낮음 — 토큰은 내부 비밀)
    3. **역방향 롤백 절차**: 새 토큰 배포 후 롤백 시, current=구토큰·previous=신토큰으로 **되돌리는 순서**를 명문화(게이트웨이를 먼저 구토큰으로 → 서비스 current 복귀 → 신토큰 previous 정리). 무절차 롤백 시 401 폭발 위험.
- [ ] **서비스별 개별 토큰** — bible 전용 토큰(글로벌 토큰 금지), env/시크릿 매니저 주입.
- [ ] (권장) mTLS·네트워크 정책(게이트웨이/내부망만 접근) 병행 — 인프라 트랙.

## 2. 배포 설정(컷오버)
- [ ] bible-service: `QTAI_BIBLE_INBOUND_ENABLED=true`, `QTAI_BIBLE_PERSISTENCE_ENABLED=true`, DB URL(=공유 모놀리식 DB, Inc4 전), `QTAI_BIBLE_GATEWAY_SHARED_TOKEN`
- [ ] 게이트웨이: `GATEWAY_BIBLE_URI`, `GATEWAY_BIBLE_SHARED_TOKEN`(서비스와 **동일 값** — 불일치 시 전량 401)
- [ ] 라우트 순서 확인(bible < monolith catch-all) — 회귀 테스트로 고정됨

## 3. 관측·롤백
- [ ] 감사 트레일: SYSTEM 호출 INFO 로그(`com.qtai.audit.bible`)를 감사 appender로 라우팅
- [ ] CircuitBreaker 폴백(bibleCb → 503 표준 envelope) 동작 확인
- [ ] 롤백 절차: 게이트웨이 라우트 설정만 되돌리면 모놀리식 복귀(병존, Inc5까지)

## 4. 후속 증분
- [x] Inc3b(grace window) · Inc3c-2(`external/bible` HTTP 클라이언트+어댑터, 기본 inprocess) 구현
- [x] **재시도(retry)** — `BibleServiceClient`에 일시 오류(5xx·IO) 재시도(4xx 무재시도, maxAttempts 상한 가드) + `BibleHttpClientConfiguration` 통합 테스트(Inc3d 준비 #403 + 보강 회수).
- [ ] **Circuit Breaker — 의존성 결정 선행(Lead/인프라)**: 모놀리식엔 resilience4j도 spring-cloud BOM도 없음 → CB 도입 시 ① `spring-cloud-starter-circuitbreaker-resilience4j`(BOM 포함) 추가 또는 ② `io.github.resilience4j:resilience4j-circuitbreaker` 버전 핀(게이트웨이 Resilience4j 버전과 정합). **새 의존성/버전 관리 결정이라 Lead/인프라 승인 후 도입**(비차단 — 재시도+타임아웃으로 일시 장애는 커버, CB는 지속 장애 fast-fail 보강). 실패 기록은 EXTERNAL_API_FAILURE만(4xx 제외).
- [ ] **소비자(qt/note/study) 계약 테스트** — 오너 협의(Inc3d 컷오버).
- [ ] Inc3d: `mode=http` 전환 + 양측 토큰 동기화(`qtai.bible.client.gateway-token` = bible-service `gateway.shared-token`)
- [ ] Inc4: DB-per-service(시드 이전, `glossary_terms` FK 제거)
- [ ] Inc5: 모놀리식 bible 도메인 제거(Strangler 완료)
