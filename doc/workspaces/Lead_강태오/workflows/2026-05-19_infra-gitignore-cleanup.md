# 2026-05-19 .gitignore 추가 + 빌드 산출물 untrack — 작업 계획

## 배경
- 직전 작업(`2026-05-19_package-structure-and-comments`)에서 `qtai-server/.gradle/`, `qtai-server/bin/` 등 빌드 산출물 164개가 git에 추적된 채로 master에 올라감 (해당 리포트 §위험 1번).
- `.gitignore`가 루트·`qtai-server/` 어디에도 없음.
- 브랜치 전환 시 Gradle daemon 잠금으로 `git checkout` 실패 위험. 다음 단계인 도메인별 PR 진행 전 반드시 차단해야 함.

## 목표 (W1 / Foundation Lock-in)
- 루트 + `qtai-server/`에 `.gitignore` 표준화
- 추적 중인 빌드 산출물 164개 untrack (로컬 파일은 유지)
- 후속 도메인 PR(`feature/member-...`)에서 잠금 충돌 0건

## 작업 범위
1. **`/.gitignore`** (신규) — IDE/OS 공통 (`.idea/`, `*.iml`, `.vscode/`, `.DS_Store`, `Thumbs.db`, `*.log`)
2. **`/qtai-server/.gitignore`** (신규) — Gradle/Java 전용 (`.gradle/`, `build/`, `bin/`, `out/`, `*.class`, `**/jacoco.exec`)
3. **`git rm -r --cached qtai-server/.gradle qtai-server/bin`** — index에서만 제거, 로컬 빌드 캐시 유지
4. `flutter-app/.gitignore`는 본 PR 범위 외 (Flutter 표준 별도 PR로 분리)

## 비범위
- 도메인 코드 변경 없음
- 빌드/CI 설정 변경 없음
- `flutter-app/` 손대지 않음

## 검증 계획
- `git ls-files | grep -E "(\.gradle/|/bin/)"` 결과 0건
- `git diff --cached --name-only | wc -l` = 166 (신규 2 + 삭제 164)
- 로컬 `qtai-server/.gradle/`, `qtai-server/bin/` 디렉토리는 디스크에 그대로 남아 있어야 함 (untrack ≠ 삭제)
- CI: Requirements Guard v3.1, Gitleaks, Spectral, Docker Compose 전 게이트 통과

## 리스크
- **다른 팀원의 pull 영향**: 머지 후 팀원이 `git pull` 받으면 working tree에서 `bin/`, `.gradle/`이 사라짐 → PR 본문에 "pull 후 `./gradlew build` 재실행" 안내 명시.
- **Eclipse 사용자**: `bin/`을 IDE가 자동 재생성하므로 영향 없음. `.classpath`, `.project`는 본 PR에서 다루지 않음 (Eclipse 사용자 발생 시 별도 처리).

## 참고
- CLAUDE.md §9 (관련 없는 리팩터링 금지)
- 직전 리포트: `reports/2026-05-19_package-structure-and-comments_report.md` §위험 1번, §다음 1번
