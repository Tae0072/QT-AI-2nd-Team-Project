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
| `domain/praise/internal/MemberPraiseSongRepository.java` | 수정 | `deleteByPraiseSongId(Long)` 추가 — 곡 삭제 시 회원 참조 행 cascade 정리(FK RESTRICT 회피) |
| `domain/praise/internal/PraiseService.java` | 수정(코드리뷰) | `delete()` cascade 정리, `listAdmin()` 잘못된 status → INVALID_INPUT(400), 클래스 Javadoc에 service-bible 동기화 계획·삭제 정책 명시 |

### admin-server (test) — 코드리뷰 반영

| 파일 | 변경 유형 | 내용 |
|---|---|---|
| `domain/praise/internal/PraiseServiceTest.java` | 신규 | update(성공/없음), delete(cascade 순서 InOrder·없음 시 참조 미삭제), listAdmin(필터/전체/INVALID_INPUT) 7건 |
| `domain/praise/web/AdminPraiseControllerTest.java` | 신규 | MockMvc standalone: list/create(201)/update(200)/delete(204)/404 P0001/400 C0002/401 M0002/403 M0003/AD0003 9건 |

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
| admin-server-sync-rules | `update`/`delete`/`listAdmin`은 관리자 콘솔(AD-05) 전용 운영 기능 → 규칙 ②에 따라 admin-server에만 둔다. service-bible의 `PraiseService`는 이 세 메서드를 의도적으로 포함하지 않음(사용자 앱에 큐레이션 변경 경로가 없음). 향후 사용자 경로가 생기면 service-bible을 원본으로 승격 후 admin-server가 따라간다. `PraiseService` 클래스 Javadoc에 동기화 계획 명시 |
| **삭제 정책(코드리뷰 반영)** | **cascade-delete.** `member_praise_songs.praise_song_id`는 `fk_mps_praise_song`(RESTRICT) — 회원이 저장한 곡을 물리 삭제하면 FK 위반 → **500 발생(확정 버그)**. `delete()`에서 `memberPraiseSongRepository.deleteByPraiseSongId(id)`로 회원 참조 행을 같은 트랜잭션 안에서 먼저 정리한 뒤 곡을 삭제한다. 회원 저장 행은 곡을 가리키는 순수 참조 메타데이터(F-09: 서버는 메타데이터만 저장, 사용자 작성 콘텐츠 아님)이므로 함께 정리해도 콘텐츠 손실이 아니며, 04 §4.7.6의 `204 No Content` 계약을 유지하고 관리자가 항상 삭제 가능 |
| **listAdmin status 400(코드리뷰 반영)** | 기존 `PraiseSongStatus.valueOf(status)`는 정의되지 않은 값에 `IllegalArgumentException` → 500. `try/catch`로 감싸 `BusinessException(INVALID_INPUT)`(400, C0002)로 매핑. 빈 문자열도 전체 조회로 처리 |
| Flutter displayTitle | 04 §4.6.4 계약상 `POST /me/praise-songs` 요청 필수 필드. 기존 코드는 `sourceType: 'CURATION'`(잘못된 enum + 불필요 필드) 전송 → 제거하고 `displayTitle` 추가 |

---

## 빌드 검증

| 게이트 | 결과 |
|---|---|
| `npm run typecheck` (admin-web) | ✅ 통과 |
| `npm run build` (admin-web) | ✅ 통과 |
| `./gradlew :admin-server:compileJava` | ✅ 통과 |
| `./gradlew :admin-server:test --tests "com.qtai.domain.praise.*"` | ✅ 통과 (PraiseServiceTest 7 + AdminPraiseControllerTest 9, 총 16건) |

---

## 후속 작업

- `feature/praise-admin-crud` PR → dev 머지 후 P5a 찬양 숨김은 v1.1 이후 별도 브랜치로 처리
- hide 구현 시: `AdminPraiseController`에 `POST /{id}/hide` 엔드포인트 + `praiseSongs.ts` `hidePraiseSong` 행 액션 연결
