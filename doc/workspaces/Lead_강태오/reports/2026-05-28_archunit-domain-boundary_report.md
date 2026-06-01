# ArchUnit 도메인 경계 자동 검증 테스트 리포트

- 작업자: Lead 강태오
- 날짜: 2026-05-28
- PR: #136 (test/archunit-domain-boundary → dev)
- 관련: CLAUDE.md §3, §4, §10

---

## 1. 배경

CLAUDE.md §10에서 "도메인 간 금지 import" 테스트를 필수로 요구한다. 수동 코드 리뷰만으로는 도메인 경계 위반을 놓칠 수 있으므로, `gradlew build` 실행 시 자동으로 검증되는 ArchUnit 테스트를 추가했다.

---

## 2. 구현 내용

### 2.1 의존성 추가

| 파일 | 변경 |
|------|------|
| build.gradle.kts | `testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")` 추가 |

### 2.2 테스트 규칙 (29개)

| 규칙 | 수량 | 설명 |
|------|------|------|
| 규칙 1: internal 접근 제한 | 13개 | 각 도메인의 internal 패키지는 해당 도메인만 접근 가능 |
| 규칙 2: web 교차 의존 금지 | 13개 | 각 도메인의 web 패키지는 다른 도메인의 web에 의존 금지 |
| 규칙 3: Controller→Repository 금지 | 1개 | Controller가 Repository를 직접 호출하지 않는다 |
| 규칙 5: api→internal 의존 금지 | 1개 | api 패키지는 internal 구현에 의존하지 않는다 |
| (규칙 4: 삭제됨) | — | 규칙 1과 중복 + false positive 문제로 삭제 |

### 2.3 파일 위치

```
qtai-server/src/test/java/com/qtai/architecture/
└── DomainBoundaryArchTest.java    (203줄)
```

---

## 3. 트러블슈팅

### 3.1 1차 빌드 실패 (2개 규칙)

| 실패 규칙 | 원인 |
|-----------|------|
| 규칙 2 (web 전체) | `noClasses().that().resideInAnyPackage(allWeb).should().dependOnClassesThat().resideInAnyPackage(allWeb)` — 같은 도메인 web 내부 참조도 위반으로 잡힘 |
| 규칙 4 (internal 전체) | 같은 패턴. AdminUserRepository → AdminUser (둘 다 admin.internal) 등 1069건 false positive |

### 3.2 해결

- **규칙 4**: 13개 per-domain 규칙(규칙 1)이 이미 동일 검증을 정확하게 수행 → 삭제
- **규칙 2**: per-domain 방식으로 재작성. `webDoesNotDependOnOtherDomainWeb(domain)` 헬퍼가 해당 도메인을 제외한 나머지 web 패키지만 금지 목록에 넣음

### 3.3 2차 빌드 결과

29개 규칙 전부 PASS, BUILD SUCCESSFUL (2회 검증)

---

## 4. 효과

- `gradlew build` 한 번이면 도메인 경계 위반이 자동 검출된다
- 새 도메인 추가 시 DOMAINS 배열에 이름만 넣으면 internal + web 규칙이 자동 생성된다
- Controller → Repository 직접 호출, api → internal 역방향 의존도 빌드에서 잡힌다

---

## 5. 변경 파일 요약

| 파일 | 변경 | 줄 수 |
|------|------|-------|
| build.gradle.kts | ArchUnit 의존성 추가 | +2 |
| DomainBoundaryArchTest.java | 신규 생성 | +203 |
| **합계** | 2 files changed | +205 |
