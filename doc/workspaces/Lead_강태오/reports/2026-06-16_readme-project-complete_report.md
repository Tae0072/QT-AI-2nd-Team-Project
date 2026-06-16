# 리포트 — README 프로젝트 완료 정리

- 작성자: Lead 강태오
- 날짜: 2026-06-16
- 관련 워크플로우: `workflows/2026-06-16_readme-project-complete.md`
- 관련 F-ID: 전체(F-01~F-16) 기준 메타 정리

## 1. 결과 요약

두 저장소 README 끝에 기존 본문을 유지한 채 "변경 이력(진행 순서)"과 "프로젝트 완료 현황(2026-06-16)" 섹션을 추가했다.

| 저장소 | 기준 브랜치 | 기존 줄 수 | 추가 후 줄 수 |
| --- | --- | --- | --- |
| 문서(2nd-Team-Project) | `origin/master` | 151 | 206 |
| 구현(QT-AI-2nd-Team-Project) | `origin/dev` | 224 | 284 |

## 2. 추가한 내용

- **변경 이력**: 2026-05-12 방향 확정 → 05-19 패키지 표준 → 06-07 music 도메인 → 06-08 MSA 전환 → 06-10 시스템 인증 분리 → 06-11 관리자 ID 로그인까지 진행 순서표.
- **완료 현황**:
  - 서버: `lib-common` + `service-user(8081)`/`service-bible(8082)`/`service-note(8083)`/`service-ai(8084)` + `admin-server(8090)` + nginx 게이트웨이(8080).
  - 도메인 16종: member, notification, mission, bible, qt, study, music, praise, qtvideo, note, sharing, report, ai, admin, audit, appversion.
  - 클라이언트: `flutter-app`(Android), `admin-web`(React+Vite).
  - 인증 3원화: 사용자 RS256 카카오 / 관리자 자체 ID·비밀번호 / 시스템 HS256 `SYSTEM_BATCH`.
  - 인프라: Docker Compose + 로컬 한정 k8s.
  - 기준 문서 버전: 요구사항 v3.5, 아키텍처 v1.3, API v1.7, ERD v2.2.

## 3. 검증 (2회 이상 점검)

1. **기존 본문 보존 확인**: `origin/master`/`origin/dev` README 원본을 그대로 두고 끝에만 append. 본문 라인 수 변동 없음.
2. **사실 검증**: 모듈·포트·도메인은 `settings.gradle.kts`와 각 service `domain/*` 디렉터리로 확인. 버전은 각 문서 헤더(`v3.5`/`v1.3`/`v1.7`/`v2.2`)로 확인.
3. **금지 패턴 점검**: 구현 README append에서 Requirements Guard 금지어(SSE/Kafka/javax/church_/ESV/NIV/개역개정/lyrics_text/audio_url 등) 비포함. 부정·제외 문맥(로컬 한정 k8s 등)만 사용.

## 4. 후속

- 두 PR은 `dev`(구현)/`master`(문서) 대상으로 생성. 보호 브랜치 직접 push 없음.
- 적용 명령은 `apply-recipe.md` 참조.
