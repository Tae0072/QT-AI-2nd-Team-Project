## 구현 내용
<!-- 이 PR에서 해결하거나 추가하는 변경 사항을 간략하게 설명하세요. -->

## 관련 이슈 / 타스크
<!-- Closes #000 / Fixes #000 -->

## 변경 유형
- [ ] 기능 추가 (feat)
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

## 테스트 체크리스트
<!-- feat / fix / refactor 타입은 아래 항목 필수 — 미충족 시 Claude가 REQUEST_CHANGES -->
- [ ] 단위 테스트(Unit Test) 작성 완료 및 `./gradlew test` 로컬 통과
- [ ] 통합 테스트(Integration Test) 작성 완료
      또는 미작성 사유: <!-- 예: 외부 의존성(Google OAuth) 특성상 Mock 처리 -->
- [ ] docs / chore 타입은 해당 없음 (위 항목 무시)

## 테스트 방법
<!-- 어떻게 테스트했는지 설명 (Unit / Integration / 수동) -->