# 2026-06-01 노트 나눔 공유 API 구현 (F-10)

## 목표
노트를 나눔 피드에 공유하는 `POST /api/v1/notes/{noteId}/share` API를 구현한다. 스냅샷 방식으로 공유 시점의 내용을 복사한다.

## 작업 내용
1. **SharingPost.publish() 팩토리** — 닉네임/제목/본문/카테고리/구절 스냅샷 복사
2. **PublishNoteRequest DTO** — `confirmNicknamePublic=true` 필수(`@AssertTrue`), `commentsEnabled` 선택
3. **SharingPostService.publish()** — 노트 조회(`GetNoteUseCase`) → 중복 검증(`findByNoteId`) → 닉네임 조회(`GetMemberUseCase`) → 스냅샷 INSERT
4. **SharingPostController** — `POST /api/v1/notes/{noteId}/share` (201 Created)
5. **ErrorCode** — `DUPLICATE_SHARING_POST(S0003, 409)` — 기존 S0002(COMMENT_NOT_FOUND) 중복 해소
6. **Race condition 방어** — `saveAndFlush` + `DataIntegrityViolationException` catch (DDL `note_id UNIQUE` 존재)
7. **publish 단위 테스트** — 정상 공유 + 중복 공유 예외 2건 추가
8. **기존 테스트 업데이트** — 생성자 인자 변경 반영

## 범위
- 브랜치: `feature/note-share`
- PR: (Open, 리뷰 대기)
- 커밋: `feat(sharing): 노트 나눔 공유 API 구현 — POST /api/v1/notes/{noteId}/share`
- 변경: 11파일 267줄
- 관련: F-10 닉네임 나눔 공간

## 리뷰 BLOCK 대응 (3건)
- ErrorCode S0002 중복 → S0003으로 변경
- Race condition → `saveAndFlush` + `DataIntegrityViolationException` catch
- 테스트 누락 → publish 정상/중복 2건 추가

## 미해결
- PR 머지 대기
- Flutter 공유 버튼 UI 후속 PR

## 담당
- Lead 강태오 (T)
