# 리포트 — MSA RestClient 통합 ② note→qt readability (2026-06-10)

## 요약

RestClient 통합 두 번째 호출 쌍. 노트 작성/수정 시 "이 QT 본문을 읽을 수 있는가"를 확인하던 임시 `GetQtUseCaseMock`(`NoteQtClient` 구현)을, service-bible(qt)의 본문 조회 엔드포인트를 호출하는 실제 어댑터로 교체했다. 더불어 어댑터마다 반복되던 인증 헤더 전달을 lib-common 공용 헬퍼로 추출했다.

## 변경 파일 (6)

신규
- `lib-common/.../common/security/ServiceCallAuthForwarder.java` — 요청 Authorization을 서비스 간 호출에 전달하는 공용 헬퍼
- `service-note/.../domain/note/client/qt/NoteQtRestClientAdapter.java` — qt readability HTTP 어댑터
- `service-note/.../test/.../note/client/qt/NoteQtRestClientAdapterTest.java`(6)

수정
- `service-note/.../domain/note/client/bible/GetBibleVerseRestClientAdapter.java` — 자체 인증 전달 메서드 제거, 공용 헬퍼 사용(중복 제거)
- `service-note/.../test/.../note/NoteApiSecurityIntegrationTest.java` — `@MockBean NoteQtClient`·`GetBibleVerseUseCase`로 cross-service HTTP 격리

삭제
- `service-note/.../domain/note/client/qt/GetQtUseCaseMock.java`

## 설계 근거

- 통신: RestClient 동기(회의록 §3). 인증: 사용자 요청 맥락이라 JWT 그대로 전달(§5/§81).
- service-bible 변경 없음: 기존 `GET /api/v1/qt/passages/{id}`(없으면 404)를 readability 확인에 재사용. qt는 service-bible 소속이라 `qtai.services.bible-base-url` 사용.
- 오류: `RestClientException`만 캐치 → 404=`QT_PASSAGE_NOT_FOUND`/403=`FORBIDDEN`/그 외=`EXTERNAL_API_FAILURE`. `toBodilessEntity`로 상태코드만 확인.

## 검증 결과

- `:lib-common:build :service-note:build` → **BUILD SUCCESSFUL (21s)**.
- service-note 테스트 44개 전부 통과(실패 0). qt 어댑터 6 · bible 어댑터 6(헬퍼 적용 후 유지) · 통합 테스트 7(@MockBean 격리로 회복).
- 첫 푸시 체크리스트 자가점검: 광범위 catch 없음, 금지 토큰 없음, 삭제 Mock 코드 참조 없음(javadoc 1줄).

## 비고 — 다음 작업의 선행 결정

service-ai→bible 등 **배치/시스템(SYSTEM_BATCH) 호출**은 전달할 사용자 JWT가 없어 서비스 간 시스템 인증 메커니즘 결정이 선행돼야 한다(Lead 보류). 그동안은 사용자 요청 맥락 읽기 쌍을 이 패턴으로 계속 진행한다.
