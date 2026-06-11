# admin-server 복사본 동기화 규칙

- 작성: 2026-06-11 강태오 (Lead) — 코드리뷰 2026-06-10 TODO 4
- 배경: `admin-server`는 **의도된 모놀리식 복사본**(약 529파일)이다. 단일 DB를 공유하며 admin-web의 백엔드 역할을 한다. 그런데 "어느 쪽이 원본인가" 규칙이 없어 드리프트가 시작됐다(V30/V32 마이그레이션 번호 충돌 사례, 스케줄러 javadoc 차이 — `doc/2026-06-10_서버_코드리뷰.md` §감사 1·3).

## 규칙 (3줄)

1. **도메인 로직 수정은 항상 도메인 서비스(service-user/bible/note/ai)가 원본이다.** admin-server의 동일 코드는 원본을 따라간다(복사 반영). admin-server 쪽만 고치는 핫픽스 금지.
2. **admin-server 고유 기능(admin 컨트롤러·관리자 배치·admin-web 전용 API)만 admin-server에서 직접 수정한다.** 이 영역은 도메인 서비스에 복사하지 않는다.
3. **스키마(Flyway 마이그레이션)는 admin-server가 단독 소유한다.** 다른 모듈(`service-*`)에 마이그레이션 파일을 두지 않는다. 위반 사례: `service-bible`의 죽은 마이그레이션 `V30__create_qt_video_clips.sql`(PR #456 유입, Flyway 의존성이 admin-server에만 있어 미실행 — 진짜는 admin-server V32. service-bible에 Flyway를 켜는 순간 체크섬 충돌. 삭제 담당: 김태혁, P2-1).

## 운용 메모

- 도메인 서비스 코드를 수정한 PR은 본문에 "admin-server 복사본 반영 여부"를 한 줄로 명시한다(반영 / 해당 없음).
- 일일 배치 소유는 도메인 서비스다(코드리뷰 2026-06-10 결정 A, PR #468). admin-server 복사본 스케줄러는 토글 off가 기본값이며 수동/관리자 트리거 경로만 유지한다.
- 장기적으로 admin-server 슬림화(admin 고유 기능만 남기기)는 별도 결정 사항으로 남긴다.
