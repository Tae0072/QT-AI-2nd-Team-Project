# 2026-06-05 P0 Critical 일괄 수정 결과 보고

전체 코드 리뷰에서 식별된 **P0 9건 전부 수정 완료** — 10개 브랜치/10개 커밋, 서버 전체 테스트 836개 그린.
워크플로우: `workflows/2026-06-05_p0-critical-fixes-batch.md` (브랜치 체인·머지 순서 포함)

## 이슈별 결과

| # | 이슈 | 커밋 | 핵심 변경 |
|---|------|------|----------|
| P0-1 | 관리자 API 전멸(ADMIN_ROLE_* 미발급) | 8acb277, 47bc1a1 | admin api에 `verifyAnyRole` 신설, ai/audit/report web 7개 진입점을 admin_users DB 2차 검증으로 통일. adminId에 members.id 대신 admin_users.id 기록(FK 오기록 동시 해소). AD 코드 403 매핑. **실JWT 발급→호출 종단 통합테스트 신설**(기존 테스트가 결함을 못 잡던 원인 제거) |
| P0-2 | 00:05 시딩이 어제 본문 시딩 | 8ebec2d | qt api에 내부 배치 전용 날짜 직접 조회(`findContentContextByDate`) 신설, 시딩이 사용자 노출 정책(STALE_FALLBACK)을 우회. 본문 부재 시 `TODAY_QT_PASSAGE_NOT_FOUND` 기록. SU 수집 00:02로 분리. 테스트 프로파일 SU 실서버 호출 차단(flaky 해소) |
| P0-3 | qt_passage_verses 미작성(파이프라인 단절) | eda6a2b | 수집 시 bible api로 절 범위→verse id 매핑 저장(갱신 시 교체), startup 백필로 과거 누락분 보강(멱등). 책 식별 native SQL 1건을 bible api 경유로 교체(MSA 대비) |
| P0-4 | Today QT 해설·시뮬레이터 단절(서버) | b9df400 | study에 경량 가용성 포트 신설(verseIds 전달로 역호출 순환 차단), qt가 캐시 밖에서 enrich(승인 시점 변화 캐시 박제 방지, 실패 시 MISSING/false fallback). 본문 부재 DISABLED→MISSING 정정 |
| P0-5 | 댓글 목록 탈퇴자 404 + N+1 | 1c8fd01 | member api에 활성 회원 일괄 조회 신설(탈퇴자는 예외가 아닌 누락), 댓글 목록 1회 벌크 + "(탈퇴한 회원)" 폴백 |
| P0-6 | 노트 삭제↔나눔글 미연동 | b4e0f21 | sharing에 `MarkSourceNoteDeletedUseCase` 신설(독립 빈 — 순환 차단), 노트 삭제와 같은 tx에서 `source_note_unshared_at` 기록(멱등). 게시글은 유지+안내(Lead 결정) |
| P0-7 | 신고 처리 빈 껍데기 | 57a280e | RESOLVED+HIDE_TARGET+POST → 모더레이션 강제 숨김(신규 포트, 같은 tx). notifyReporter → REPORT_RESULT 알림(eventKey 멱등, 실패 비차단). audit_logs 기록(REPORT_RESOLVE/REJECT, admin_users.id) |
| P0-8 | 미래 본문 선노출 | 85a7bd6 | getPassage 미래 날짜 404(존재 은닉), getContentContext published=(qtDate≤오늘 KST) 실계산 — study 3곳의 기존 게이트가 비로소 동작. ai 사전 생성 경로는 영향 없음 |
| P0-9 | Flutter 기동·로그인 차단 + 계약 단절 | 48d465b | iOS 카카오 URL scheme 추가(로그인 불가 해소), debug cleartext 허용(dev API 차단 해소), 가짜 본문 fallback 제거, simulatorStatus/hasExplanation/draftNoteId 파싱 + §6 버튼 게이팅(미지 값 MISSING 방어) |

## 검증

- 서버 전체 스위트 **836 tests / 0 failures** (체인 시점별 + 영향 도메인별 반복 실행)
- 회귀 방지 테스트 약 40개 신규/강화 — 특히 P0-1은 "권한 수동 주입 테스트가 결함을 가리던" 구조 자체를 종단 테스트로 교정
- **Flutter 실검증 완료** (Flutter 3.44.1 / Dart 3.12.1, D:\TOOL\flutter):
  - `flutter analyze` → No issues found
  - `flutter test` → **111 tests passed**
  - `flutter build apk --debug` → **Built 성공** (debug 매니페스트 cleartext 머지 정상 확인)
  - analyze가 호출처 누락 1건(`bible_providers.dart`의 제거된 fallbackReferenceText 인자)을 잡아 함께 수정 후 재검증 — Flutter 커밋(09c1808)에 포함
  - iOS plist 변경은 Windows에서 빌드 불가라 구조(XML)만 검증 — Mac/CI 빌드에서 최종 확인 필요

## 사용자 영향 요약 (머지 후)

- 관리자 콘솔: 신고 처리·감사 조회·AI 자산 승인 정상 동작 (이전: 전부 403)
- 오늘 QT: 당일 해설 job이 당일 생성 → 04:00 사이클 정상화, 자동 수집 본문도 해설·학습 콘텐츠 파이프라인 가동
- 콘텐츠 승인 시 앱의 해설·시뮬레이터 버튼이 실제로 활성화(서버·클라 양방향)
- 탈퇴 회원 댓글이 있어도 댓글 목록 정상, 노트 삭제 시 나눔 글에 원본 삭제 안내
- iOS 카카오 로그인 가능, dev 환경 API 호출 가능, 잘못된 본문 노출 경로 제거

## 남은 사항 / 후속

- **P1~P3는 조치 계획서대로 미착수** — 다음 배치에서 진행
- 시뮬레이터·해설 상세 "화면"은 Flutter 미구현(버튼 활성화는 정책대로, 탭 시 준비 안내) — 화면 연결 후속
- 신고 COMMENT/AI 대상 숨김은 해당 도메인 포트 신설 시 확장(주석 명시)
- applicationId TODO·release 서명은 배포 키 준비와 함께 별도 처리
- 머지 순서는 워크플로우 문서의 체인 순서를 따를 것. 전 체인 머지 후 dev 전체 테스트 1회
