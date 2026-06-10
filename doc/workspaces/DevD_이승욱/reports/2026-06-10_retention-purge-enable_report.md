# 2026-06-10 retention 정리 배치 게이트 + 활성화 (task 2) — 결과 보고

## 요약
탈퇴 회원 보존기간 만료 정리 배치에 활성화 게이트(`qtai.retention.purge.enabled`)를 추가하고, admin 검증 통합(task 1) 완료 후 운영에서 켰다. 기본 off(개발/테스트 데이터 보호), 운영 on. 정리 로직 무변경. service-user 인증 BE 5건 완료.

## 산출물
| 파일 | 설명 |
|------|------|
| `admin-server/.../batch/MemberRetentionPurgeBatch.java` | `@ConditionalOnProperty(qtai.retention.purge.enabled=true)` 게이트 |
| `admin-server/.../application.yml` | base 기본값 off(`${QTAI_RETENTION_PURGE_ENABLED:false}`) |
| `admin-server/.../application-prod.yml` | 운영 활성화(`:true`, env 오버라이드 가능) |
| `admin-server/.../batch/MemberRetentionPurgeBatchGateTest.java` | 게이트 3건 |

## 변경 성격
- **안전 게이트화**: 비가역 hard delete 배치를 명시 토글로 통제. 기존 always-on(모든 환경)을 기본 off + 운영만 on으로 전환.
- **선행 조건 충족**: admin 연결 회원 제외 판정(`VerifyAdminRoleUseCase`)이 신뢰 가능해진 뒤 활성화.

## 검증
- 게이트 3 + 컨텍스트 로드 1 = **4건 0 실패**. true→등록 / false·미설정→미등록 / off 기본에서 컨텍스트 정상.

## 미해결 / 후속
- 운영 제어: `QTAI_RETENTION_PURGE_ENABLED`. (선택) purgedCount 관측.
- **service-user 인증 BE 5건(task1~5) 전부 완료.**
