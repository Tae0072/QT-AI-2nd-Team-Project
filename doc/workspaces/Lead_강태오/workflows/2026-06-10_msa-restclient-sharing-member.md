# 2026-06-10 MSA RestClient 통합 ③ — sharing→member (작성자 닉네임)

> RestClient 통합 세 번째 호출 쌍. 사용자 요청 맥락 읽기. 기준: 회의록 2026-06-09 §3·§5 + CLAUDE.md §4·§5·§9.
> 핸드오프 `HANDOFF_2026-06-10_msa-restclient-sessionA-sharing-member.md`를 실행한 결과.

## 1. 범위

service-note의 sharing 도메인이 나눔글/댓글에 작성자 닉네임을 채울 때 쓰던 임시 `sharing/client/member/GetMemberUseCaseMock`을 service-user(member) HTTP 호출 어댑터로 교체.

- ① **service-user**: 활성 회원 공개 프로필 벌크 엔드포인트 `GET /api/v1/members?ids=1,2,3` 추가(이미 있던 `getActivePublicProfiles`에 위임). 단건 `/members/{id}`는 기존 재사용.
- ② **service-note**: `sharing/client/member/MemberRestClientAdapter implements GetMemberUseCase`.
  - `getMemberPublic(id)` → `GET /api/v1/members/{id}`
  - `getActivePublicProfiles(ids)` → `GET /api/v1/members?ids=`(빈 입력은 호출 없이 빈 결과)
  - `getMember(id)`(전체 계약) → **공개 뷰만**: `/members/{id}` 호출 후 공개 필드만 채우고 비공개(email·status·role 등) null(프라이버시). sharing은 닉네임만 사용.
  - 인증: `ServiceCallAuthForwarder`(요청 JWT 전달). 오류: `RestClientException`만 캐치 → 404=`MEMBER_NOT_FOUND`/그 외=`EXTERNAL_API_FAILURE`.
- ③ **Mock 삭제** + `application.yml`에 `qtai.services.user-base-url`(env) 추가.

## 2. 사실 관계(조사)

- `member.api.GetMemberUseCase`: `getMember`(전체), `getMemberPublic`(공개), `getActivePublicProfiles`(활성만, 순서 보장 없음, 탈퇴/정지 제외).
- 사용처: `SharingPostService.getMember(memberId).nickname()`, `CommentService.getMemberPublic(...)`+`getActivePublicProfiles(authorIds)`.
- service-user `MemberService`가 이미 활성-only 필터로 두 메서드 구현 → 컨트롤러 노출만 추가.

## 3. 체크리스트

- [x] 브랜치 `feature/msa-restclient-sharing-member`(origin/dev-msa=f24d4ff 분기)
- [x] ① 벌크 엔드포인트 + MockMvc 테스트(SecurityIntegrationTest +2)
- [x] ② 어댑터 구현
- [x] ③ Mock 삭제 + user-base-url
- [x] 테스트: 어댑터 MockRestServiceServer 7 + service-user 통합 8 GREEN, NoteApiSecurityIntegrationTest 영향 없음(7 유지)
- [x] 빌드 GREEN(`:service-user:build :service-note:build`, 37s) · 2~3회 검토
- [ ] PR → dev-msa 첫 푸시 자동머지

## 4. 검증
```powershell
$env:JAVA_HOME='D:\workspace\tools\jdk\jdk-21.0.11+10'
cd D:\workspace\QT-AI-restclient\qtai-server
.\gradlew.bat :lib-common:build :service-user:build :service-note:build --no-daemon
```

## 5. 다음
- 사용자 요청 맥락 읽기 쌍 계속 가능(service-bible(qt)→note 등). 배치/SYSTEM_BATCH 호출(ai→*, user→purge)은 별도 트랙(서비스 간 시스템 인증) 완료 후 — 핸드오프 세션 B.
