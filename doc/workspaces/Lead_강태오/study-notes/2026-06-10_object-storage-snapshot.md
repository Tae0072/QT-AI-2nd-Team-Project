# 스터디 노트 — 오브젝트 스토리지로 "정적 스냅샷" 만들기 (입문자용)

> 2026-06-10, Day3 QT 오브젝트 스토리지. 매일 QT를 JSON 파일로 만들어 S3에 올리고 앱이 그 파일만 읽게 한 작업에서 배운 것.

## 1. 왜 "JSON 파일 + 오브젝트 스토리지"인가
QT는 하루에 한 번 정해지는 **읽기 전용** 콘텐츠다. 매번 서버 DB를 조회·인증하게 하면 서버 부하만 커진다. 그래서:
- 매일 00:00에 그날 QT를 **`2026-06-09.json` 같은 정적 파일**로 만들어 오브젝트 스토리지(S3/R2)에 올린다.
- 앱은 오늘 날짜 JSON URL만 읽는다 → **서버·DB·인증 불필요**. 서버 역할은 "파일 생성"으로 축소.
이게 "정적 스냅샷" 패턴이다. CDN처럼 읽기 트래픽을 서버 밖으로 빼는 것.

## 2. 포트 & 어댑터 — 저장소를 갈아끼우기
스토리지 종류(로컬 파일 / S3 / R2 / MinIO)가 바뀌어도 비즈니스 코드는 안 바뀌게 **포트(인터페이스)** 를 둔다.
- `QtSnapshotStore.store(key, json)` = "어딘가에 저장하고 위치를 돌려줘" 라는 약속만 정의(도메인 internal).
- 구현(어댑터)은 둘: 로컬파일(기본·의존성 0), S3(AWS SDK). 외부 시스템 호출이라 `client/`에 둔다(CLAUDE.md §4).
- **정확히 하나만 켜지게**: `@ConditionalOnProperty(name="qt.snapshot.storage", havingValue="local", matchIfMissing=true)` / `havingValue="s3"`. 프로퍼티 값으로 빈 1개 선택 → 충돌·모호함 없음. (이전에 `@ConditionalOnMissingBean`을 @Component에 달았다가 빈이 안 뜬 적이 있어, 프로퍼티 게이트가 더 안전.)

## 3. S3 호환 = 코드 1벌로 MinIO·R2·S3 다 붙는다
AWS SDK v2의 `S3Client`에 **endpoint override + path-style**만 켜면 로컬 MinIO·Cloudflare R2·AWS S3가 전부 같은 코드로 동작한다.
- 로컬 시연: MinIO(내 PC의 S3 흉내) — 클라우드 계정·비용 없이 오브젝트 스토리지 학습.
- 배포: endpoint/키만 env로 바꾸면 진짜 S3/R2.
자격증명은 **env로만** 받고 **로그에 절대 안 남긴다**(키 노출 사고 방지).

## 4. "공유 파일에는 내 정보가 섞이면 안 된다"
실수하기 쉬운 지점: 기존 `TodayQtResponse`엔 `draftNoteId`(내 임시노트)·`cacheStatus`(런타임 상태)가 들어있다. 이건 **사용자마다 다른 값**이라 모두가 읽는 공유 JSON에 넣으면 안 된다.
- 그래서 **member-agnostic**(사용자 무관) 소스인 `findContentContextByDate(date)`로 콘텐츠만 뽑아 스냅샷을 만든다.
- 테스트로 `json.doesNotContain("draftNoteId","cacheStatus","memberId")`까지 못박았다. "없어야 하는 게 없다"를 테스트하는 습관.

## 5. 시간 관련 두 함정
- **스케줄러 끄기**: `@Scheduled`는 `@EnableScheduling`이 있으면 무조건 돈다. 테스트·기동에서 외부 스토리지를 건드리지 않게 `@ConditionalOnProperty(qt.snapshot.enabled, 기본 false)`로 빈 자체를 게이트.
- **노출정책 우회**: QT는 00:00~04:00엔 "어제 것"을 보여주는 정책(STALE_FALLBACK)이 있다. 내부 배치가 그걸 그대로 쓰면 "어제 본문"을 스냅샷하는 버그 → **날짜 명시 조회**(`findContentContextByDate`)로 우회.
- 테스트의 시계는 `Clock.fixed(...)`로 고정해 날짜 경계(예: 15:30Z = 익일 09시 KST)를 결정적으로 검증.

## 6. 작은 함정: ObjectMapper와 java.time
`new ObjectMapper()`는 `LocalDate`를 직렬화 못 해 터진다(JavaTimeModule 미등록). 운영에선 Spring Boot가 모듈 등록된 ObjectMapper를 주입해 괜찮지만, **단위 테스트에서 직접 만든 ObjectMapper는 `registerModule(new JavaTimeModule())` + `disable(WRITE_DATES_AS_TIMESTAMPS)`** 로 운영과 똑같이 맞춰야 "2026-06-09" 같은 ISO 문자열이 나온다.

## 7. Day3 트랙 선택의 교훈
"무엇을 먼저 하느냐"도 설계다. k8s는 CI 가드가 막고(정책 결정 필요), admin-server는 구조 결정이 필요하고, RestClient 통합은 여러 서비스 런타임이 필요하다. **자체 완결적이고 모호함이 없는 것(QT 스토리지)부터** 하면 빠르고 정확하게 머지된다 — 막히는 의존을 가진 작업은 결정이 선 뒤로 미룬다.
