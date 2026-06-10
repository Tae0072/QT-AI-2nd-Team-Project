# 2026-06-10 retention 정리 배치 게이트 + 활성화 (task 2)

## 목표
탈퇴 회원 보존기간(2년) 만료 정리 배치(`MemberRetentionPurgeBatch`)에 **활성화 게이트**(`qtai.retention.purge.enabled`)를 추가하고, admin 검증 통합(task 1)이 끝난 지금 운영에서 켠다. service-user 인증 BE 작업 5건의 마지막.

## 배경
- 배치는 admin-server에 있고 `@Scheduled(03:00 KST)`로 `PurgeExpiredWithdrawnMembersUseCase.purgeExpired()` 호출(hard delete). 스케줄링은 `config/SchedulingConfig`(@EnableScheduling)로 활성.
- **게이트 부재**: 기존엔 게이트가 없어 admin-server가 뜨는 모든 환경(dev/local 포함)에서 배치 빈이 생성·스케줄됐다. hard delete는 비가역이라 개발/테스트 데이터 삭제 위험.
- 관리자 연결 회원 제외는 `MemberRetentionPurgeService.hasAdminLink()`가 `VerifyAdminRoleUseCase.getActiveAdmin()`(admin-server 내 실제 `AdminService`)으로 판정 — `ADMIN_USER_NOT_FOUND`만 "연결 없음". 이 판정이 신뢰 가능(task1 admin 검증 통합)해진 뒤 활성화하는 게 [선행] 조건이었다.

## 작업 내용
1. `MemberRetentionPurgeBatch`에 `@ConditionalOnProperty(prefix="qtai.retention.purge", name="enabled", havingValue="true")` 추가 — 속성 true일 때만 빈 등록(미설정/false → 자동 정리 미동작).
2. base `application.yml`: `qtai.retention.purge.enabled: ${QTAI_RETENTION_PURGE_ENABLED:false}` — **안전 기본값 off**(dev/local/test 데이터 보호).
3. `application-prod.yml`: `qtai.retention.purge.enabled: ${QTAI_RETENTION_PURGE_ENABLED:true}` — **운영 활성화**(env로 끌 수 있음).

## 범위
- 브랜치: `feature/retention-purge-enable` (base: `dev-msa`)
- 변경: 배치 1(게이트) + yml 2(base off·prod on) + 테스트 1. 정리 로직(`MemberRetentionPurgeService`) 무변경.

## 검증
- `:admin-server:test` — `MemberRetentionPurgeBatchGateTest` 3건(true→빈 등록 / false→미등록 / 미설정→미등록) + `AdminServerApplicationTest`(컨텍스트 로드, 게이트 off 기본에서 배치 빈 없이 정상) = **4건 0 실패**.
- 기존 배치 참조 테스트 없음(게이트로 빈이 빠져도 무영향). 테스트 프로파일 retention 미설정 → 기본 off로 배치 미생성.

## 미해결 / 후속
- 운영 배포 시 `QTAI_RETENTION_PURGE_ENABLED`로 환경별 제어 가능. prod 기본 on, dev/local off.
- (선택) 정리 실행 결과(purgedCount) 메트릭/알림.

## 담당
- DevD 이승욱 (service-user 인증 BE — 탈퇴 정책 후속)
