# 2026-06-10 dev 관리자 시드 + 관리자 인증 API 계약 — 결과 보고

## 요약
service-user 인증 BE의 선행 토대 2건 완료. ① dev 전용 관리자 계정 시드(멱등 repeatable + dev 전용 location 격리로 운영 미적용). ② 관리자 카카오 인증 API 응답 계약 제안서(김지민 합의용). 코드 로직 무변경(시드·설정·문서).

## 산출물
| 파일 | 설명 |
|------|------|
| `admin-server/.../db/dev-seed/R__seed_dev_admin_account.sql` | (신규) 멱등 관리자 시드 — members ADMIN + admin_users, placeholder 파라미터화 |
| `admin-server/.../application-local.yml`, `application-dev.yml` | flyway.locations에 db/dev-seed + placeholders 추가(prod/demo 제외) |
| `contracts/2026-06-10_admin-kakao-auth-api-contract.md` | 관리자 인증 API 계약 제안(요청/응답/오류/합의포인트) |
| `workflows/...`, `reports/...` | 작업 문서 |

## 검증
- 시드 H2(MODE=MySQL) 직접 실행: 신규 생성·재실행 멱등(중복0)·기존 USER 승격 모두 통과.
- dev-only 격리: prod/demo는 migration-only 상속 → 운영 미적용.

## 핵심 발견(후속 task 영향)
- 단일 DB Flyway 소유자 = **admin-server**(시드/스키마는 여기에).
- admin-server엔 **임의 memberId admin 검증 시스템 엔드포인트 부재**(AdminController는 GET /me만) → task 1은 service-user 어댑터 + admin-server 수신 엔드포인트 **양쪽** 필요.
- `MemberRetentionPurgeBatch`는 `qtai.retention.purge.enabled` 게이트가 **아직 없음** → task 2는 게이트 추가 후 활성화.

## 후속
- task 1(VerifyAdminRole RestClient 양쪽) → task 3(관리자 인증) → task 2(retention 게이트+활성화).
- task 5 계약 김지민 합의 후 확정.
