# 리포트 — 찬양 큐레이션 CRUD (2026-06-11)

| 항목 | 내용 |
|---|---|
| 브랜치 | `feature/praise-admin-crud` (base = `origin/dev`) |
| F-ID | F-06(관리자 운영 기능), F-09(찬양 큐레이션) |
| 관련 화면 | AD-05 찬양 큐레이션 관리, M-03 내 찬양 |
| PR 대상 | `dev` |

---

## 구현 요약

P5a(찬양 숨김) 처리를 위해 admin-server `AdminPraiseController`가 필요했으나, 숨김은 v1 범위 제외로 확정됐다. 대신 **수정(PATCH) + 삭제(DELETE)** CRUD를 완성하고, admin-web FE와 Flutter M-03 displayTitle 계약 불일치도 함께 수정했다.

---

## 변경 파일

### admin-server (backend)

| 파일 | 변경 유형 | 내용 |
|---|---|---|
| `domain/praise/api/DeletePraiseUseCase.java` | 신규 | `void delete(Long adminId, Long praiseSongId)` UseCase 인터페이스 |
| `domain/praise/api/UpdatePraiseUseCase.java` | 신규 | `PraiseResponse update(Long adminId, Long praiseSongId, PraiseUpdateRequest)` UseCase |
| `domain/praise/api/dto/PraiseUpdateRequest.java` | 신규 | `@NotBlank title`, `artist`, `licenseNote` record |
| `domain/praise/api/dto/PraiseResponse.java` | 수정 | 6-arg → 8-arg (`licenseNote`, `updatedAt` 추가) |
| `domain/praise/api/ListPraiseUseCase.java` | 수정 | `listAdmin(String status, Pageable)` 메서드 추가 |
| `domain/praise/internal/PraiseSong.java` | 수정 | `update(title, artist, licenseNote)` 엔티티 메서드 추가 |
| `domain/praise/internal/PraiseService.java` | 수정 | `listAdmin`·`update(@Transactional)`·`delete(@Transactional)` 구현, `toResponse` 8-arg 갱신 |
| `domain/praise/internal/PraiseSongRepository.java` | 수정 | 중복 `findAll(Pageable)` 선언 제거 (JpaRepository 기본 제공) |
| `domain/praise/web/AdminPraiseController.java` | 신규(패키지 포함) | GET/POST/PATCH/{id}/DELETE/{id}, `requireOperator` 패턴(OPERATOR), hide 엔드포인트 없음 |

### admin-web (FE)

| 파일 | 변경 내용 |
|---|---|
| `src/api/praiseSongs.ts` | `UpdatePraiseSongRequest` 타입 + `updatePraiseSong`/`deletePraiseSong` 함수, `hidePraiseSong` stub 제거 |
| `src/pages/PraiseSongsPage.tsx` | 수정 Modal(title·artist·licenseNote) + 삭제 Popconfirm(danger) + "작업" 컬럼 |

### Flutter

| 파일 | 변경 내용 |
|---|---|
| `lib/features/mypage/services/mypage_repository.dart` | `saveMyPraiseSong(praiseSongId, displayTitle)` — `displayTitle` 파라미터 추가, `sourceType` 제거 (04 §4.6.4 계약 정합) |
| `lib/features/mypage/screens/praise_screen.dart` | `song.title`을 `displayTitle`로 전달 |

---

## 설계 결정 사항

| 결정 | 내용 |
|---|---|
| 숨김(hide) v1 제외 | `PraiseSong.hide()` 엔티티 메서드는 이미 존재하므로 v1.1에서 컨트롤러 엔드포인트만 추가하면 됨 |
| admin-server-sync-rules | CRUD 로직은 `domain.praise` 내 `PraiseService`에 구현. `AdminPraiseController`는 admin 전용 웹 레이어만 담당 (CLAUDE.md §1 준수) |
| 물리 삭제 | `praiseSongRepository.delete(song)` — 큐레이션 메타데이터는 소프트 삭제 불필요 (04 §4.7.6 `204 No Content` 명세) |
| Flutter displayTitle | 04 §4.6.4 계약상 `POST /me/praise-songs` 요청 필수 필드. 기존 코드는 `sourceType: 'CURATION'`(잘못된 enum + 불필요 필드) 전송 → 제거하고 `displayTitle` 추가 |

---

## 빌드 검증

| 게이트 | 결과 |
|---|---|
| `npm run typecheck` (admin-web) | ✅ 통과 |
| `npm run build` (admin-web) | ✅ 통과 |
| `./gradlew :admin-server:compileJava` | 이전 세션 확인 완료 |

---

## 후속 작업

- `feature/praise-admin-crud` PR → dev 머지 후 P5a 찬양 숨김은 v1.1 이후 별도 브랜치로 처리
- hide 구현 시: `AdminPraiseController`에 `POST /{id}/hide` 엔드포인트 + `praiseSongs.ts` `hidePraiseSong` 행 액션 연결
