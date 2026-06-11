# 2026-06-11 마이페이지 PR(#478) 리뷰 WARN 후속 처리 (bugfix/member-dashboard-review-warn)

## 목표·배경
PR #478(마이페이지 피드백 5건)이 자동 리뷰 WARN 6건과 함께 머지됨 — WARN은 머지 후속 처리 합의.
후속 수정이 구 브랜치에 남아 있어 dev에서 새 브랜치로 재구성했다. 공통 문서(04/07)는 원본 보존
원칙에 따라 본문 수정분을 되돌리고 "개정 추가" 절(append) 방식으로 전환했다(2026-06-11 T 결정).

## 작업 내용 (WARN 6건 대응)
1. memberId 검증 방어 — `MyPageController.requireMemberId`(null→401) + 비인증 테스트
2. Mock 교체 파급 — `recalculateAllEnrolled()` 운영 호출자 없음 확인, 배치 도입 시 SYSTEM_BATCH 토큰 선행 필요를 Coordinator javadoc에 명시
3. 광역 catch — 어댑터는 `RestClientException` 한정(원래 좁음). `loadStats`의 `catch(Exception)`은 위젯별 부분 실패 격리 정책상 의도된 설계(클래스 javadoc 문서화)로 유지
4. cause 유실 — 어댑터에서 던지기 전 원인 `log.warn` 보존(토큰·헤더 미포함, §9), 실패 봉투 로그 추가
5. 타임아웃 — 공용 빌더 무한 대기 확인 → 어댑터 단위 connect 2s/read 3s 명시(부분 실패 허용 경로라 빠른 실패). 테스트는 MockRestServiceServer 보존용 package-private 생성자(applyTimeouts=false)
6. 04 명세 정합 — **개정 추가 절** 방식으로 §4.1.2/§4.1.5/§6.2 잠금 서술 대체 + v1.8 이력. 07도 #478의 본문 직접 수정을 원본(v3.5)으로 복원하고 "개정 추가 — v3.6" 절로 재기록

## 범위
- 브랜치: `bugfix/member-dashboard-review-warn` (origin/dev 분기), PR 대상 dev
- 서버 5파일(컨트롤러·어댑터·코디네이터·테스트2) + 문서 2파일(04/07 개정 추가) + workspace 문서

## 검증
- `:service-user:test` 79건 전체 통과 (신규: 비인증 401 1건)
- 공통 문서: 본문 diff 없음(07은 원본 복원), 추가 절만 append

## 미해결 / 후속
- lib-common 공용 `restClientBuilder` 타임아웃 부재는 전 서비스 공통 이슈 — 별도 PR/협의 필요
- 배치형 미션 재계산 도입 시 SYSTEM_BATCH 토큰 기반 달력 호출 경로 필요

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
