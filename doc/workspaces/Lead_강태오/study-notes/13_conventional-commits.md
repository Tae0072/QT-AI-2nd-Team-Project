# Conventional Commits — 커밋 메시지 규칙

> **왜 배워야 하나:** QT-AI 프로젝트는 CLAUDE.md에서 Conventional Commits를 사용하도록 명시했다. 커밋 메시지를 정해진 형식으로 작성하면 변경 이력을 쉽게 파악할 수 있다. 노션에서 Git 기초(merge, rebase, reset)를 배웠지만, 커밋 메시지 규칙은 다루지 않았다.

---

## 1. Conventional Commits가 뭔가?

커밋 메시지를 **일정한 형식**으로 작성하는 규칙이다. 커밋만 봐도 "이 변경이 기능 추가인지, 버그 수정인지, 설정 변경인지" 바로 알 수 있다.

## 2. 기본 형식

```
<타입>(<범위>): <설명>

[본문]

[꼬리말]
```

### 예시

```
feat(member): 닉네임 7일 변경 잠금 구현

F-10 요구사항에 따라 닉네임 변경 후 7일 잠금을 적용한다.
잠금 중 변경 시도 시 409 NICKNAME_CHANGE_LOCKED를 반환한다.

Refs: F-10
```

## 3. 타입 종류

| 타입 | 의미 | 예시 |
|------|------|------|
| `feat` | 새 기능 추가 | `feat(qt): Today QT 조회 API 구현` |
| `fix` | 버그 수정 | `fix(member): 닉네임 중복 체크 누락 수정` |
| `refactor` | 리팩터링 (기능 변경 없음) | `refactor(bible): 쿼리 최적화` |
| `test` | 테스트 추가/수정 | `test(ai): F-15 Q&A 차단 테스트 추가` |
| `docs` | 문서 변경 | `docs: CLAUDE.md 금지 기술 목록 갱신` |
| `chore` | 빌드, 설정, 도구 변경 | `chore: Gradle 버전 업그레이드` |
| `style` | 코드 포맷, 세미콜론 등 | `style: 줄바꿈 정리` |
| `ci` | CI/CD 설정 변경 | `ci: GitHub Actions 테스트 캐시 추가` |

## 4. 범위 (Scope)

괄호 안에 변경 범위를 적는다. QT-AI에서는 도메인 이름을 범위로 사용한다:

```
feat(member): ...     → member 도메인 관련
fix(bible): ...       → bible 도메인 관련
chore(infra): ...     → 인프라/설정 관련
test(qt): ...         → qt 도메인 테스트
```

## 5. QT-AI 브랜치명과의 관계

QT-AI Git 규칙(09_Git_규칙.md)에서 브랜치명도 비슷한 형식을 따른다:

```
브랜치: feature/member-nickname-lock
커밋:   feat(member): 닉네임 7일 변경 잠금 구현

브랜치: fix/qt-cache-null-pointer
커밋:   fix(qt): 캐시 비어있을 때 NPE 수정

브랜치: chore/infra-docker-compose
커밋:   chore(infra): Docker Compose MySQL 볼륨 설정
```

## 6. 좋은 커밋 메시지 작성 팁

```
❌ 나쁜 예시:
- "수정"
- "기능 추가"
- "update code"
- "WIP"

✅ 좋은 예시:
- "feat(member): 카카오 로그인 시 신규 회원 자동 생성"
- "fix(qt): 00:00-04:00 캐시 미스 시 빈 응답 반환 수정"
- "test(ai): 금지 질문 유형 차단 테스트 5건 추가"
```

## 7. F-ID 연결

QT-AI에서는 기능 PR에 F-ID를 명시한다:

```
feat(qt): Today QT 조회 API 구현

F-02 요구사항에 따라 오늘의 QT 조회 엔드포인트를 구현한다.

Refs: F-02
```

## 8. 참고 자료

- Conventional Commits 공식: https://www.conventionalcommits.org/ko/v1.0.0/
- Angular 커밋 컨벤션: https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit
