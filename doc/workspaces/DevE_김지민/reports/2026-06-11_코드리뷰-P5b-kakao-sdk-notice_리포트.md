# Report — 2026-06-11 코드리뷰 P5b admin-web 카카오 SDK 키 미설정 안내

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김지민 (admin-web FE) |
| 브랜치 | `feature/admin-web-kakao-sdk-notice` (base `origin/dev`) |
| 출처 TODO | `2026-06-10_코드리뷰_TODO_김지민.md` TODO 5 (P5b) |
| 관련 F-ID | F-04 |

## 변경 내용

`VITE_KAKAO_JS_KEY` 미설정 시 클릭해야만 알던 에러(`kakao.ts` throw)를 **사전 안내**로 개선.

- `LoginPage.tsx`: `KAKAO_JS_KEY` import → `kakaoKeyMissing` 감지 → 미설정이면 **경고 Alert**(`.env`에 `VITE_KAKAO_JS_KEY` 설정 안내, dev에선 개발용 토큰 우회 안내 포함) + **카카오 로그인 버튼 비활성화**(헛클릭 방지).

## 검증 결과

- `npm run typecheck` + `npm run build` 통과. (참고: #505 dev-bypass 깨짐으로 한때 막혔으나, 강태오님 #510 롤백[#504 상태]으로 dev 복구 후 검증 완료.)
- 실데이터 E2E 불필요(순수 UI 안내).

## 비고 / 후속

- 같은 브랜치에 **P5a 찬양 admin 엔드포인트 요청서**(`workflows/2026-06-11_P5a-praise-admin-endpoint-요청.md`)도 포함됨(별도 협의용 문서).
- 로드맵 다음: **P5c 재적용**(#508 code-split이 #510 롤백에 휩쓸려 revert됨) → P4.
