# 2026-06-10 MSA RestClient 통합 ② — qt readability (note→qt)

> 작성: 강태오(Lead, AI 보조). RestClient 통합 두 번째 호출 쌍. 기준: 회의록 2026-06-09 §3·§5 + CLAUDE.md §4·§5·§9.
> 선행 PR #435(note→bible)에서 확립한 패턴을 재사용한다.

## 0. 범위 결정 배경

다음 후보였던 **service-ai→bible**은 해설 생성 **배치(SYSTEM_BATCH) 맥락**이라 전달할 사용자 JWT가 없다. 팀의 시스템 토큰 방식은 `role=SYSTEM_BATCH` JWT인데 발급기(`JwtProvider`, 개인키)는 service-user에만 있어 service-ai가 스스로 토큰을 못 만든다. 아카이브(95ecfb4)는 게이트웨이가 신원 헤더를 주입하던 (폐기된) 구조라 인증 모델을 못 쓴다. 시스템 토큰 문서(`DevC_강상민/.../2026-06-05_...system-token-server-call.md` §124)가 "service_accounts/shared-secret 인증"을 후속 과제로 미뤄둔 상태다.

→ **Lead 결정(2026-06-10): 서비스 간 시스템 인증 결정 전까지는 사용자 요청 맥락의 읽기 쌍부터 진행.** 이번엔 note→qt.

## 1. 이번 PR 범위

- ① **lib-common**: 인증 전달 공용 헬퍼 `ServiceCallAuthForwarder.forward(HttpHeaders)` 추출. 사용자 요청의 `Authorization`을 서비스 간 호출에 그대로 전달. 기존 note→bible 어댑터도 이 헬퍼를 쓰도록 정리(중복 제거).
- ② **service-note**: `note/client/qt/NoteQtRestClientAdapter`가 `NoteQtClient.validateReadable(memberId, qtPassageId)`를 HTTP로 구현. service-bible(qt)의 본문 조회 `GET /api/v1/qt/passages/{id}`를 재사용해 readability만 확인(`toBodilessEntity`).
- ③ **Mock 삭제**: `note/client/qt/GetQtUseCaseMock`(이름과 달리 `NoteQtClient` 구현) 제거.
- ④ **테스트 격리**: `NoteApiSecurityIntegrationTest`에 `@MockBean NoteQtClient`·`GetBibleVerseUseCase` 추가(자체 통합 테스트는 cross-service HTTP를 타지 않게).

> service-bible 쪽은 **변경 없음** — `/api/v1/qt/passages/{id}`가 이미 존재(없으면 404)해 readability 확인에 그대로 쓴다. qt는 service-bible에 있으므로 base URL은 `qtai.services.bible-base-url`을 재사용.

## 2. 설계 결정

- 인증: 사용자 요청 맥락 → 요청 JWT 그대로 전달(§5/§81), 공용 헬퍼 사용.
- 오류(CLAUDE.md §9): `RestClientException`만 구체 캐치. 404→`QT_PASSAGE_NOT_FOUND`, 403→`FORBIDDEN`, 그 외→`EXTERNAL_API_FAILURE`.
- readability는 본문 바디를 쓰지 않으므로 `toBodilessEntity`로 상태코드만 본다(과다 페치 방지).
- 입력 가드: `qtPassageId` null/<1 또는 `memberId` null이면 HTTP 호출 없이 `QT_PASSAGE_NOT_FOUND`(기존 Mock 동작 보존).

## 3. 체크리스트

- [x] 선행: `git checkout -b feature/msa-restclient-note-qt origin/dev-msa`(2eec40c, PR #435 포함)
- [x] ① `ServiceCallAuthForwarder` + bible 어댑터 정리
- [x] ② `NoteQtRestClientAdapter` 구현
- [x] ③ `GetQtUseCaseMock` 삭제
- [x] ④ `NoteApiSecurityIntegrationTest` 스텁(@MockBean)
- [x] 테스트: qt 어댑터 6 + bible 어댑터 6(헬퍼 적용 후 유지) + 통합 7 → service-note 44개 GREEN
- [x] 빌드 GREEN(`:lib-common:build :service-note:build`, 21s) · 2~3회 검토
- [ ] PR → dev-msa, 첫 푸시 자동머지

## 4. 검증

```powershell
$env:JAVA_HOME='D:\workspace\tools\jdk\jdk-21.0.11+10'
cd D:\workspace\QT-AI-restclient\qtai-server
.\gradlew.bat :lib-common:build :service-note:build --no-daemon
```

## 5. 다음(아직 막힘)

- **서비스 간 시스템 인증 결정**이 service-ai→bible/qt/study, user→note/praise/report 등 배치/시스템 호출의 선행 조건. 후보: lib-common 공유 시크릿(HMAC) 기반 단명 `SYSTEM_BATCH` 토큰 발급기. 결정 후 일괄 진행.
- 사용자 요청 맥락 읽기 쌍은 계속 이 패턴으로 진행 가능(예: sharing→member).
