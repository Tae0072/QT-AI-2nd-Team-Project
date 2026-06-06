# 2026-06-01 노트 나눔 공유 API 구현 — 결과 보고

## 요약
노트를 나눔 피드에 공유하는 `POST /api/v1/notes/{noteId}/share` API 구현. 스냅샷 방식으로 닉네임/제목/본문/카테고리/구절을 복사. `confirmNicknamePublic=true` 필수, 중복 공유 409.

## 산출물

| 파일 | 설명 |
|------|------|
| `SharingPost.java` | `publish()` 팩토리 메서드 추가 |
| `PublishNoteRequest.java` | `@AssertTrue confirmNicknamePublic` + `commentsEnabled` |
| `PublishNoteUseCase.java` | TODO → 실제 인터페이스 구현 |
| `SharingPostService.java` | `publish()` 구현 + `GetNoteUseCase`/`GetMemberUseCase` 의존 추가 |
| `SharingPostRepository.java` | `findByNoteId` 추가 |
| `SharingPostController.java` | `POST /api/v1/notes/{noteId}/share` 엔드포인트 |
| `ErrorCode.java` | `DUPLICATE_SHARING_POST(S0003, 409)` — S0002 중복 해소 |
| `SharingPostServiceTest.java` | 생성자 인자 업데이트 |
| `SharingPostControllerTest.java` | 생성자 인자 업데이트 |

## 검증
- `gradlew build` — BUILD SUCCESSFUL
- `SharingPostServiceTest` — publish 정상/중복 2건 + 기존 테스트 전체 통과
- PR Guard — 11파일/267줄, 금지 항목 없음, 도메인 경계 준수

## 리뷰 BLOCK 대응 (3건)
- ErrorCode S0002 중복 → S0003으로 변경
- Race condition → `saveAndFlush` + `DataIntegrityViolationException` catch
- 테스트 누락 → publish 정상/중복 2건 추가

## 미해결
- PR 머지 대기
