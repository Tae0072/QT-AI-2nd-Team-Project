# Report — 2026-06-11 코드리뷰 P4 admin-web 토큰 보관 재검토


| 항목      | 내용                                                        |
| ------- | --------------------------------------------------------- |
| 담당자     | 김지민 (admin-web FE)                                        |
| 브랜치     | `docs/admin-web-token-storage-review` (base `origin/dev`) |
| 출처 TODO | `2026-06-10_코드리뷰_TODO_김지민.md` TODO 4 (P4)                 |


## 배경

관리자 토큰을 `localStorage`(`qtai_admin_token`/`qtai_admin_refresh_token`)에 저장 — XSS 발생 시 토큰 탈취 가능. 의도적 임시 설계이나 "졸업(전환) 시점"이 미정이었음.

## 이번 변경

- `tokenStorage.ts` 상단에 **보안 메모 주석 보강**: XSS 위험 인지 + 운영 전 HttpOnly 전환 계획 + 완료 조건 + 전환 시점 강태오 결정 대기 명시. (코드 로직 무변경, 주석만)

## 전환 계획 (운영 전 완료 조건)

1. **목표**: access/refresh 토큰을 **HttpOnly 쿠키**로 이전 → JS가 토큰에 접근 못 해 XSS 탈취 위험 제거.
2. **필요 변경**: 서버 측 CORS `credentials` 허용 + 쿠키 발급/만료 처리. → **이승욱(service-user)·강태오 협의 필요**. P2 refresh 흐름과 묶으면 변경 1회로 끝남.
3. **FE 영향**: 저장 계층(`tokenStorage.ts`)만 교체하면 호출부(AuthContext·client.ts) 그대로. client.ts는 쿠키 자동 전송(withCredentials)으로 Authorization 헤더 첨부 제거.
4. **완료 조건**: **운영 배포 전**.

## 결정 (2026-06-11)

- **전환 시점 = 데모 후(운영 배포 직전)** 로 결정.
  - 근거: 데모는 통제된 시연 환경이라 localStorage XSS 실위험 낮음 / 전환은 서버(CORS·쿠키)+FE 동시 변경이라 데모 직전엔 시연 안정성(§7.2) 우선 / 일정상 데모 전엔 기능 안정화 집중, 보안 하드닝은 운영 전 P2 refresh와 묶어 1회.
  - ⚠️ 예외: 데모가 **공개/실데이터** 노출이면 데모 전으로 앞당긴다.
  - 강태오(Lead) 최종 확인 후 운영 전 **전환 PR**(서버 CORS credentials·쿠키 + FE `tokenStorage` 교체) 진행.

## 비고

- 현재(dev/시연) localStorage 유지는 의도적이며, 본 PR은 **위험 인지 명문화 + 전환 계획 기록**이 목적(코드리뷰 P4의 1단계).

