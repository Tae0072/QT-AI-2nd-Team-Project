# 리포트 — MSA RestClient 통합 ③ sharing→member 닉네임 (2026-06-10)

## 요약

세 번째 호출 쌍. 나눔글/댓글 작성자 닉네임을 채우던 임시 `GetMemberUseCaseMock`(sharing)을 service-user(member) HTTP 호출 어댑터로 교체했다. service-user에는 N+1 방지용 벌크 공개프로필 엔드포인트를 추가했다.

## 변경 파일 (6)

신규
- `service-note/.../domain/sharing/client/member/MemberRestClientAdapter.java`
- `service-note/.../test/.../sharing/client/member/MemberRestClientAdapterTest.java`(7)

수정
- `service-user/.../domain/member/web/MemberController.java` — `GET /api/v1/members?ids=` 벌크 공개프로필 엔드포인트 추가(기존 `getActivePublicProfiles` 위임)
- `service-user/.../test/.../user/SecurityIntegrationTest.java` — 벌크 200·미인증 401 (+2, 총 8)
- `service-note/src/main/resources/application.yml` — `qtai.services.user-base-url`(env)

삭제
- `service-note/.../domain/sharing/client/member/GetMemberUseCaseMock.java`

## 설계 근거

- 인증: 나눔 피드·댓글 조회는 사용자 요청 맥락 → 요청 JWT 그대로 전달(`ServiceCallAuthForwarder`).
- 프라이버시: 서비스 간 호출은 공개 뷰만. `getMember`(전체 계약)도 공개 엔드포인트를 호출해 공개 필드만 채우고 비공개(email·status·role)는 null. SharingPostService는 닉네임만 사용해 영향 없음.
- 벌크: 활성 회원만(탈퇴/정지 제외)·순서 보장 없음 — 호출자(CommentService)는 누락 id에 자체 폴백.
- 오류: `RestClientException`만 캐치 → 404=`MEMBER_NOT_FOUND`/그 외=`EXTERNAL_API_FAILURE`.

## 검증 결과

- `:service-user:build :service-note:build` → **BUILD SUCCESSFUL (37s)**.
- service-user `SecurityIntegrationTest` 8(+2) · service-note `MemberRestClientAdapterTest` 7 · `NoteApiSecurityIntegrationTest` 7(영향 없음) 모두 통과.
- 첫 푸시 체크리스트 자가점검: 광범위 catch 없음, 금지 토큰 없음, 삭제 Mock 잔여 참조 없음.

## 비고

배치/SYSTEM_BATCH 호출(ai→*, user→purge)은 서비스 간 시스템 인증 메커니즘 결정 후 진행(핸드오프 세션 B). 사용자 요청 맥락 읽기 쌍은 이 패턴으로 계속.
