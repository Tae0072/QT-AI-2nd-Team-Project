# 2026-06-05 미사용 client Mock 5종 제거 — 작업 계획

## 배경
- PR #249(`chore(sharing): 미사용 토큰 공유(shares) 죽은 코드 제거`)에서 "남은 리스크 / 후속 PR"로 명시된 후속 작업.
- 기준 문서: `doc/workspaces/DevE_김지민/workflows/2026-06-05_shares-죽은코드-정리-플랜.md` §5 — shares용으로 깔렸으나 미사용으로 보이는 죽은 Mock 5종. 삭제 전 개별 grep 재확인 조건부.
- 근거 규칙: CLAUDE.md §4 "진짜 구현체 등록되면 Mock 삭제".

## 목표
- 미사용 Mock 5종 제거로 죽은 코드 0, 신규 인원 혼란 제거, ArchUnit/빌드 영향 없음 확인.

## 작업 범위 (삭제 5)
1. `sharing/client/qt/GetQtUseCaseMock.java`
2. `sharing/client/study/GetStudyUseCaseMock.java`
3. `sharing/client/member/GetMemberUseCaseMock.java`
4. `sharing/client/note/GetNoteUseCaseMock.java`
5. `note/client/member/GetMemberUseCaseMock.java`

전부 어노테이션 없는 빈 TODO 스캐폴딩(클래스 본문 0줄) — Spring bean 미등록, 컴파일 외 영향 없음.

## 비범위
- `note/client/qt/`(GetQtUseCaseMock, NoteQtClient): 플랜 §5 목록에 없음 — 건드리지 않음.
- `ai/client/qt/GetQtUseCaseMock`: 실구현 보유 + `AiQtClientContractTest`가 사용 — 별개 클래스, 유지.
- 나눔게시글(sharing-posts) 시스템 전체: 정상 완성본, 유지 (플랜 §8).

## 안전성 검증 (삭제 전, 3회)
1. **클래스명 참조 grep 전수**: `GetQtUseCaseMock|GetStudyUseCaseMock|GetNoteUseCaseMock|GetMemberUseCaseMock` → 대상 5파일 자신 외 참조 0. (test의 `AiQtClientContractTest`는 ai 도메인 동일 패키지의 별개 Mock 사용 — import 확인 완료)
2. **패키지 import grep**: `sharing.client.*`, `note.client.member` import 0건. Javadoc/주석 언급 0건 (PR #249의 SharingPostController 케이스 같은 잔존 언급 없음).
3. **실사용 주입 확인**: `SharingPostService`는 `member.api.GetMemberUseCase`, `note.api.GetNoteUseCase` 등 실제 api 인터페이스만 import. `NoteService`는 member 자체를 미사용. ArchUnit(`DomainBoundaryArchTest`)은 접근 제한 규칙만 검사 — 패키지 존재를 전제하지 않음.

## 실행 절차
1. dev 최신화 (#249 머지 커밋 3642626 포함 확인)
2. `chore/sharing-remove-unused-client-mocks` 브랜치 생성
3. `git rm` 5파일 → 빈 디렉터리(`sharing/client/` 전체, `note/client/member/`) 자동 소멸
4. 워크플로우/리포트 문서 작성 → 커밋 → PR (base: dev)

## 검증 계획
- 삭제 후 잔여 참조 grep 재확인 0건
- CI: qtai-server Build & Test, ArchUnit/Modulith 경계, Requirements Guard v3.1 전 게이트 통과
- 로컬 gradle 컴파일은 작업 환경 제약으로 미실행 — 삭제 대상이 참조 0의 빈 스캐폴딩이므로 컴파일 영향 없음, CI에서 재검증
