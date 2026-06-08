# 스터디 노트 — 배경음악 기능 + 웹/카카오 작업에서 배운 것 (2026-06-07, 입문자용)

> 이번 작업(앱 전역 배경음악, 웹 dev 도구, 카카오 웹 로그인 초안)을 하면서 나온 개념들을 입문자 기준으로 정리한다. "왜 그렇게 했는지"를 중심으로.

---

## 1. 음원을 "DB에 저장"한다는 것 (BLOB)

- **BLOB** = Binary Large OBject. 이미지·음원 같은 바이너리를 DB 컬럼에 통째로 넣는 타입.
- MySQL의 BLOB 종류: `TINYBLOB`(255B) < `BLOB`(64KB) < `MEDIUMBLOB`(16MB) < `LONGBLOB`(4GB). mp3는 수 MB라 **LONGBLOB**.
- **트레이드오프:** 간단하지만 DB가 커지고, 목록 조회 때 무거워질 수 있다. 그래서 **목록은 메타데이터만 SELECT**하고(음원 바이트 제외), 재생할 때만 바이트를 읽도록 분리했다(JPA projection 사용).
- 더 큰 규모라면 보통 음원은 **오브젝트 스토리지(S3 등)**에 두고 DB엔 URL만 저장한다. 이번엔 Lead 결정으로 DB 저장.

## 2. DB 스키마 관리 — Flyway + "환경마다 다른 DB"

- **Flyway**: `V1__...sql`, `V2__...sql`처럼 버전 붙은 SQL을 순서대로 적용해 DB 스키마를 관리하는 도구. 한 번 적용된 마이그레이션은 **수정하면 안 된다**(체크섬 깨짐) → 항상 새 버전(V26, V27…)을 추가.
- **함정: 같은 SQL이 H2와 MySQL 둘 다에서 돌아야 한다.** 이 프로젝트는 로컬=H2(메모리), 운영=MySQL. 그래서:
  - `ALTER TABLE ... ADD COLUMN a, ADD COLUMN b` (콤마 다중)는 H2에서 실패 → **컬럼마다 ALTER 문 분리**.
  - 인덱스는 `KEY` 대신 인라인 `INDEX` 사용(H2 호환).

## 3. Hibernate "schema validation" 에러 (LONGBLOB 사건)

- 운영(MySQL)은 `ddl-auto: validate` → 시작할 때 **엔티티 매핑 ↔ 실제 DB 컬럼 타입**이 맞는지 검사.
- 에러: `found [longblob], but expecting [tinyblob]`. 이유: `@Lob byte[]`는 기본 length=255라 Hibernate가 **tinyblob**을 기대하는데 DB는 longblob.
- **해결:** 엔티티 필드에 `@JdbcTypeCode(SqlTypes.LONGVARBINARY)`로 타입을 명시 → longblob과 일치. (DB/마이그레이션은 그대로 두고 엔티티만 수정.)
- 교훈: "DB는 맞는데 앱이 안 뜬다" → validate가 타입 불일치를 잡은 것. 첫 줄 `Caused by:`를 보면 원인이 보인다.

## 4. Flutter 상태관리(Riverpod) + 오디오(just_audio)

- **Riverpod**: 전역 상태/의존성 주입. `StateNotifierProvider`로 "배경음악 컨트롤러"를 앱 한 개만 두고, 어느 화면에서든 같은 인스턴스를 공유 → 탭을 옮겨도 재생 유지.
- **just_audio**: `ConcatenatingAudioSource`(여러 곡 플레이리스트) + `LoopMode.all`(반복). `AudioSource.uri(..., headers:)`로 스트리밍 URL에 JWT 헤더를 붙여 인증.
- **웹 자동재생 정책:** 브라우저는 "사용자가 한 번 클릭하기 전"엔 소리 자동재생을 막는다. → 첫 터치에 재생을 시작하도록 `Listener`로 보완. (안드로이드는 즉시 가능.)
- **아이콘은 "의도(enabled)" 기준으로:** 재생 중(playing) 기준이면 웹에서 클릭 전 "꺼짐"처럼 보임 → 설정값(enabled) 기준으로 표시.

## 5. 설정이 "유지"되는 원리

- 음악 on/off·볼륨·종류는 **서버 DB(member_settings)**에 저장(`PATCH /me/settings`)하고, 앱 켤 때마다 `GET /me/settings`로 불러옴 → 껐다 켜도/재설치해도 계정 기준 유지.
- 반대로 "마지막 듣던 곡 위치"처럼 저장 안 하는 것도 구분.

## 6. Git / PR 실전에서 배운 것

- **브랜치 이름 규칙은 CI가 강제한다.** 이 repo는 `^(feature|bugfix|hotfix|chore|release|docs|test)/...`. `feat/`는 불허 → `feature/`. (CI의 "Branch Name Convention"이 빨개지면 이거.)
- **Conventional Commits**: `feat(scope): ...`, `chore(...)`, `docs: ...`. PR 제목도 이 형식이어야 "Semantic PR Title" 통과.
- **stacked PR(쌓인 PR):** 내 작업이 아직 dev에 없는 다른 브랜치 위에 얹혀 있으면, PR을 dev로 보냈을 때 남의 커밋까지 섞인다. 해결:
  - `git worktree`로 **격리된 작업 폴더**를 만들고(현재 작업트리 안 건드림),
  - `git cherry-pick <내 커밋>`으로 **내 커밋만 dev 위에** 떼어 올린 뒤 force-push → 깔끔한 단독 PR.
- **rebase vs cherry-pick:** 둘 다 "커밋을 다른 베이스 위로 옮기기". rebase는 보통 작업트리가 깨끗해야 하고, cherry-pick은 특정 커밋만 콕 집어 옮길 때 편하다.
- **auto-merge(`gh pr merge --auto`):** 조건(체크 통과/리뷰)이 충족되면 자동 머지. 단 dev에 "required status checks"가 없으면 빌드 끝나기 전에 바로 머지될 수 있다 → green 강제하려면 브랜치 보호 설정 필요.
- **동시 작업 충돌:** 같은 repo에서 사람이 동시에 커밋하면 브랜치가 꼬인다. 이럴 때도 worktree+cherry-pick으로 "내 것만" 분리하면 남의 작업을 안 건드리고 정리할 수 있다.
- **155MB 같은 큰 파일:** GitHub은 파일당 100MB 제한(레포 총량은 경고). 바이너리가 많으면 **Git LFS**를 고려. `.gitattributes`에 `*.mp3 binary`로 줄바꿈 정규화로 인한 손상 방지.

## 7. 카카오 로그인 — SDK vs 웹(OAuth) 흐름

- 모바일은 **카카오 Flutter SDK**가 토큰을 바로 준다 → 서버에 토큰 전달(`POST /auth/kakao`).
- **웹은 다르다:** 카카오 Flutter SDK는 웹 미지원. 웹용 **카카오 JS SDK**는 `Kakao.Auth.login`(직접 토큰)을 폐지하고 `Kakao.Auth.authorize()`(인가코드 리다이렉트)만 제공.
- **OAuth Authorization Code 흐름:** 로그인 → 카카오가 우리 사이트로 `?code=...`를 붙여 리다이렉트 → 그 **코드를 서버가 카카오 토큰 엔드포인트에서 토큰으로 교환**. 코드↔토큰 교환은 키/CORS 때문에 **서버에서** 해야 한다.
- 그래서 "서버 변경 없는 웹 카카오 로그인"은 현재 불가 → **서버 OAuth(B안)**가 필요. 이건 이 프로젝트의 "서버사이드 OAuth 미사용" 결정과 충돌해서, 코드는 짜되 **DRAFT PR**로 두고 강사/Lead 검토를 받기로 함.
- **오픈 리다이렉트 방지:** 토큰 교환의 `redirect_uri`는 클라이언트가 보낸 값이 아니라 **서버 설정값**을 쓴다(보안).

## 8. "건드리지 않기"의 기술 — 조건부 컴파일

- 웹 전용 코드(`dart:js_interop`, `dart:io`)가 다른 플랫폼 빌드를 깨지 않게:
  - **조건부 import/export**: `export 'stub.dart' if (dart.library.html) 'web.dart';` → 웹에선 web 구현, 아니면 no-op 스텁.
  - 이렇게 하면 모바일 빌드 트리에 웹 전용 코드가 아예 안 들어가 빌드가 안전.
- 같은 패턴을 `file_storage`(dart:io)와 `kakao_web_login`(js_interop)에 사용.

## 9. 일하는 방식에서 배운 것

- **정책/요구사항과 충돌하면 임의로 바꾸지 말고 표시한다.** 음원 DB 저장(F-09 금지)·카카오 서버 OAuth(§1 충돌)는 둘 다 문서에 충돌을 명시하고 Lead/강사 판단을 받았다.
- **보안 민감 코드(인증)는 테스트·검증 없이 머지하지 않는다** → DRAFT.
- **작은 단위로 PR 분리**(서버/Flutter/문서, 임시 도구는 별도) → 리뷰가 쉽고 되돌리기 쉽다.
- **검증을 못 하는 환경이면(샌드박스에 Flutter/카카오 없음) 그 사실을 분명히 적고** 사용자가 실행할 명령/체크리스트를 남긴다.
