# 2026-06-05 미사용 client Mock 5종 제거 — 작업 리포트

## 요약
- PR #249 후속 작업. shares(토큰 링크 공유) 스캐폴딩과 함께 깔렸던 미사용 client Mock 5종을 제거했다.
- 변경: 삭제 5파일(-65줄) + 워크플로우/리포트 문서 추가. 코드 기능 변경 0.
- 브랜치: `chore/sharing-remove-unused-client-mocks` (base: dev, #249 머지 커밋 3642626 이후)

## 삭제 목록 (플랜 §5 전체)
| # | 파일 | 상태 |
|---|------|------|
| 1 | `sharing/client/qt/GetQtUseCaseMock.java` | 빈 TODO 스캐폴딩, 참조 0 |
| 2 | `sharing/client/study/GetStudyUseCaseMock.java` | 빈 TODO 스캐폴딩, 참조 0 |
| 3 | `sharing/client/member/GetMemberUseCaseMock.java` | 빈 TODO 스캐폴딩, 참조 0 |
| 4 | `sharing/client/note/GetNoteUseCaseMock.java` | 빈 TODO 스캐폴딩, 참조 0 |
| 5 | `note/client/member/GetMemberUseCaseMock.java` | 빈 TODO 스캐폴딩, 참조 0 |

삭제로 `sharing/client/` 패키지 전체와 `note/client/member/`가 소멸. CLAUDE.md §4 기준(client는 선택 패키지 — 타 도메인 호출 없으면 두지 않는다)에 부합.

## 검증 결과 (3회)
1. **클래스명 grep 전수**: 대상 4개 클래스명 전체 검색 → 자기 자신 외 참조 0.
   - 주의 케이스: `test/.../ai/client/qt/AiQtClientContractTest.java`가 `GetQtUseCaseMock` 사용 → **ai 도메인 동일 패키지의 별개 클래스**(실구현 보유) 확인. 삭제 대상 아님.
2. **패키지 import/주석 grep**: `sharing.client.*`, `note.client.member` import 0건, Javadoc 잔존 언급 0건. 삭제 후 재확인도 0건.
3. **실사용 경로 확인**: `SharingPostService`는 `member.api.GetMemberUseCase`·`note.api.GetNoteUseCase`(실제 api 인터페이스)만 주입. `NoteService`는 member 미사용. `DomainBoundaryArchTest`(ArchUnit)는 접근 규칙만 검사 — 패키지 부재 무영향.

## 비범위 (건드리지 않음)
- `note/client/qt/{GetQtUseCaseMock, NoteQtClient}` — 플랜 §5 목록 외. 사용 여부 별도 확인 필요 시 후속 검토.
- `ai/client/qt/GetQtUseCaseMock` — 실구현 + 계약 테스트 사용 중.

## 테스트
- chore(삭제) 타입 — 신규 테스트 면제. grep 전수 + CI(qtai-server Build & Test, ArchUnit/Modulith, Requirements Guard v3.1)로 재검증.
- 로컬 gradle 미실행 사유: 작업 환경 제약. 삭제 대상이 참조 0의 빈 스캐폴딩(클래스 본문 없음)이므로 컴파일 영향 없음.

## 남은 리스크 / 후속
- `note/client/qt/GetQtUseCaseMock`이 실제 사용 중인지(NoteQtClient와의 관계) 별도 확인 후 필요 시 정리 — 이번 범위 밖.

## Workflow
- doc/workspaces/Lead_강태오/workflows/2026-06-05_sharing-remove-unused-client-mocks.md
