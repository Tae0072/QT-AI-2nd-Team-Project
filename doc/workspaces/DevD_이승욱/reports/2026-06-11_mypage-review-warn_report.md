# 2026-06-11 마이페이지 PR(#478) 리뷰 WARN 후속 처리 — 결과 보고

## 요약
머지된 PR #478의 자동 리뷰 WARN 6건을 새 브랜치(`bugfix/member-dashboard-review-warn`, dev 분기)로 처리했다. 코드 4건(인증 가드·파급 주석·cause 로그·타임아웃)과 문서 2건(04 v1.8·07 v3.6 정합)이며, 공통 문서는 원본 보존 원칙에 따라 본문 수정 없이 "개정 추가" 절로 기록했다(07은 #478의 본문 직접 수정을 원본으로 복원 후 재기록).

## 산출물
| 파일 | 설명 |
|------|------|
| `service-user/.../member/web/MyPageController.java` | `requireMemberId` null 가드(401) |
| `service-user/.../mission/client/note/GetMeditationCalendarRestClientAdapter.java` | connect 2s/read 3s 타임아웃, 원인 로그 보존, 테스트용 생성자 분리 |
| `service-user/.../mission/internal/MissionProgressCoordinator.java` | 배치 경로에서 사용자 JWT 어댑터 사용 불가 주의 javadoc |
| `service-user` 테스트 2파일 | 비인증 401 케이스, MockRestServiceServer 보존 생성자 적용 |
| `doc/프로젝트 문서/04_API_명세서.md` | **append만**: v1.8 이력 행 + "개정 추가 (2026-06-11)" 절(§4.1.2/§4.1.5/§6.2 잠금 서술 대체) |
| `doc/프로젝트 문서/07_요구사항_정의서.md` | #478의 본문 직접 수정 원복(v3.5 원본) + "개정 추가 — v3.6" 절 append |

## 검증
- `:service-user:test` 79건 0 실패(컨텍스트 로드 포함 — 어댑터 이중 생성자 `@Autowired` 명시로 해소)
- 광역 catch(WARN 3)는 대시보드 위젯별 부분 실패 격리 정책상 의도된 설계로 유지(리뷰 회신 예정)

## 미해결 / 후속
- lib-common 공용 RestClient.Builder 타임아웃 부재(전 서비스 공통) — 별도 협의/PR
- 배치형 미션 재계산 도입 시 SYSTEM_BATCH 토큰 호출 경로 선행 필요

담당: DevD 이승욱 (Lead 강태오 계정으로 작업)
