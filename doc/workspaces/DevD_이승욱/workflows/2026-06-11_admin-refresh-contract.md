# 2026-06-11 admin refresh 계약 확정 + ADMIN role 유지 보증 (docs/member-admin-refresh-contract)

## 목표·배경
코드리뷰 TODO 5-b: admin 토큰 갱신 엔드포인트 미확정 상태였으나, "일반 사용자와 동일" 방침이 확정됨에 따라 계약 문서에 기록하고 서버 동작을 테스트로 보증한다.

## 작업 내용
- 계약 문서 `contracts/2026-06-10_admin-kakao-auth-api-contract.md`에 **개정 추가(append)** — §5에 6번 항목: 갱신 = 공용 `POST /api/v1/auth/refresh` 재사용, ADMIN role 유지 근거, admin-web 적용 가이드(인터셉터 URL, M0006/M0007 처리). 원본 본문 무수정.
- `AuthServiceTest.refresh_ADMIN_회원은_ADMIN_role로_재발급한다` 추가 — `AuthService.refresh()`가 DB role을 다시 읽어 ADMIN 유지 발급함을 고정(USER 강등 회귀 방지).
- 코드 변경 없음(기존 동작 검증·문서화만).

## 검증
- `AuthServiceTest` 전체 통과. 잔여: 김지민 vite proxy(8081) 반영 후 서버 로그로 admin 로그인 도달 확인(TODO 5-a).

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
