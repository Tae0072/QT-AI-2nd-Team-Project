# 2026-06-11 admin refresh 계약 확정 — 결과 보고

## 요약
admin 토큰 갱신은 **공용 `POST /api/v1/auth/refresh` 재사용**으로 확정(2026-06-11, 일반 사용자와 동일 방침). 서버는 이미 재발급 시 DB의 `members.role`을 다시 읽으므로 ADMIN이 유지됨을 확인했고, 회귀 방지 테스트로 고정했다. 코드 변경 없음.

## 산출물
| 파일 | 설명 |
|------|------|
| `contracts/2026-06-10_admin-kakao-auth-api-contract.md` | 개정 추가(§5-6): 갱신 엔드포인트 확정 + admin-web 적용 가이드 — 원본 무수정 |
| `AuthServiceTest.java` | `refresh_ADMIN_회원은_ADMIN_role로_재발급한다` 1건 추가 |

## 검증
- `AuthServiceTest` 통과 — `issueAccessToken(memberId, "ADMIN")` 검증(USER 강등 회귀 방지).

## 미해결 / 후속
- TODO 5-a: 김지민 vite proxy(`/api/v1/admin/auth`→8081) 반영 후 서버 로그로 도달 확인.
- admin-web 인터셉터 갱신 URL 적용은 김지민 작업(가이드는 계약 문서에 기재).

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
