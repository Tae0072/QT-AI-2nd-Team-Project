## 구현 내용

<!-- 이 PR에서 문제를 해결하거나 추가하는 변경 사항을 간략하게 설명하세요. -->

## 관련 이슈 / 타스크

<!-- Closes #000 / Fixes #000 -->

## 변경 유형

- [ ] 특정 (feat)
- [ ] 버그 수정 (fix)
- [ ] 리팩토링 (refactor)
- [ ] 테스트 (test)
- [ ] 문서 (docs)
- [ ] 인프라 / CI (chore)

## 코드 체크리스트

- [ ] `DECISIONS.md` 값과 충돌이 없음
- [ ] PostgreSQL / ZooKeeper / Tempo 코드 없음
- [ ] `application.yml` 또는 코드에 평문 Secret 없음
- [ ] Kafka envelope에 `payload` 키 대신 `data` 사용
- [ ] AI SSE 경로 `/turns` 사용 (`/messages` 아님)
- [ ] 성경 데이터: 개역개정/ESV/NIV 코드 없음
- [ ] `@Transactional` 없는 DB 변경 메서드 없음
- [ ] Kafka 이벤트 발행은 `@TransactionalEventListener(AFTER_COMMIT)`
- [ ] 테스트 코드 포함 또는 테스트 고려 이유 명시

## ai-service PR 시 추가 체크

<!-- ai-service가 아니리면 삭제 -->

- [ ] Python FastAPI 코드만 있음 (Java/Spring Boot 없음)
- [ ] Kafka envelope `data` 키 사용
- [ ] SSE 이벤트: `turn_started` → `token` → `rag_sources` → `turn_completed` → `[DONE]`

## 테스트

<!-- 어떻게 테스트했는지 설명 (Unit / Integration / 수동) -->

## 스크린샷 / 로그 (선택)

<!-- 필요시 넣어주세요 -->
