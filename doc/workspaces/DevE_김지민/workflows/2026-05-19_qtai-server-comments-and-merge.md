# 2026-05-19 ~ 05-20 qtai-server 주석 작성 + dev 머지 정리

## 한눈에 보기
| 항목 | 내용 |
|---|---|
| 작업자 | 김지민 (DevE) |
| 기간 | 2026-05-19 (주석 작성) ~ 2026-05-20 (머지 정리) |
| 브랜치 | `anno` → `dev` |
| 핵심 산출 | qtai-server 전 도메인 JavaDoc + TODO 시그니처 |

## 왜 했나
- qtai-server 패키지는 Lead가 잡은 빈 스켈레톤 (`a350629`) 상태
- 다음 도메인 PR이 코드 위 주석만 보고 책임·권한·의존성을 파악해야 함
- 헥사고날(포트/어댑터) 의도가 주석으로 박혀 있어야 분담 PR이 굴러감

## 작업 흐름

### 1일차 — 2026-05-19 (주석 작성)
1. `anno` 브랜치 분기
2. 전 도메인 `.java` 216개에 주석 추가
   - **클래스 JavaDoc**: 역할 1줄 + 권한 정책 + 의존성 블록
   - **메서드/필드 `// TODO:`**: 시그니처 + 어노테이션 힌트 (`@Entity`, `@Transactional`, `@AuthenticationPrincipal` 등)
   - **Controller**: HTTP 메서드/경로 매트릭스
   - **Mock 어댑터**: `@Primary` 자동 비활성화 트리거 명시
3. 커밋 `e55684e 주석 작성`
4. `origin/anno` 푸시

### 2일차 — 2026-05-20 (dev 머지 정리)
1. `git pull origin dev`
2. `git merge dev` → 충돌 72개 발생
3. 진단: 충돌 파일 전부 빌드 부산물 (`bin/*.class` 69개 + `.gradle/*.bin` 2개 + `.lock` 1개)
   - `.java` 충돌 **0개** → 주석 작업 무사
   - dev쪽에 `cd880fe chore(infra): add .gitignore and untrack build artifacts` 가 들어와 있어서 "한쪽 수정 / 한쪽 삭제" 충돌
4. 해소: 충돌 전부 "삭제" 측으로 받기
   ```powershell
   git rm -rf qtai-server/bin
   git rm -rf qtai-server/.gradle
   git commit -m "anno에 dev 머지 — 빌드 산출물(bin, .gradle) 정리"
   ```
5. 커밋 `11af6ff`, `origin/anno` 푸시

## 주석 컨벤션
- JavaDoc 한국어 작성 (팀 가이드)
- 클래스 1줄 요약 + 빈 줄 + 상세 블록
- `// TODO:` 는 시그니처/어노테이션 힌트 용도로만
- 약어 풀어쓰기는 패키지 진입점에서 한 번만

## 검증
- [x] 216개 `.java` 주석 추가
- [x] 컴파일 통과 (시그니처 변경 없음)
- [x] dev 머지 충돌 0
- [x] origin/anno 푸시 완료
- [x] anno → dev PR 머지

## 다음 단계
1. dev → master 반영 후 leaf 도메인부터 구현 PR
2. 첫 구현 PR에서 TODO 시그니처 → 실제 메서드 치환 패턴 검증

## 관련 커밋
| SHA | 메시지 | 비고 |
|---|---|---|
| `e55684e` | 주석 작성 | 216 파일, +1,360 / -269 |
| `11af6ff` | anno에 dev 머지 — 빌드 산출물(bin, .gradle) 정리 | 충돌 72개 해소 |
