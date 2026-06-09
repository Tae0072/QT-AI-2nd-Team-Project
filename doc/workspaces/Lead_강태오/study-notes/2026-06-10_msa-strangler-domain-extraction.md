# 스터디노트 — MSA 도메인 추출(Strangler)과 서비스 경계 (2026-06-10)

> service-note(note·sharing·report 제출)를 모놀리식에서 떼어내며 배운 것. 입문자 기준 정리.

## 1. Strangler(스트랭글러) 패턴 — "한 번에 다 뜯지 않는다"

모놀리식을 한 번에 새 구조로 바꾸면 위험하다. 대신 **기존 코드는 그대로 두고**, 도메인을 하나씩 새 모듈로 **복사·이전**한 뒤, 모든 게 옮겨지면 그때 원본을 지운다. 이번엔 `qtai-server/src`(모놀리식)는 손대지 않고 note·sharing·report를 `service-note`로 복사했다. 검증: `git status -- qtai-server/src`가 비어 있으면 원본을 안 건드린 것이다.

비유: 교살무화과(strangler fig)가 숙주 나무를 한 번에 베지 않고 천천히 감싸 대체하는 데서 온 이름.

## 2. "import"만 보면 의존을 놓친다 — FQN(전체경로) 참조 함정

다른 도메인을 쓰는 방법은 두 가지다.

```java
import com.qtai.domain.member.api.GetMemberUseCase;   // (1) import 문
private final GetMemberUseCase getMemberUseCase;

private final com.qtai.domain.notification.api.SendNotificationUseCase sender;  // (2) FQN 직접
```

`grep "import com.qtai.domain"`만 돌리면 (2)를 **놓친다**. 실제로 sharing은 notification을 FQN으로만 의존하고 있어서, import 검색에선 안 보였다. ⇒ cross-service 의존을 찾을 땐 import가 아니라 **패키지 경로 자체**(`com.qtai.domain.notification.`)를 본문 전체에서 grep 해야 한다.

## 3. 도메인 경계 — "api 계약만 가져오고, 구현은 Mock으로"

규칙(CLAUDE.md §3·§4): 다른 도메인의 **`internal`(Entity·Service·Repository)은 절대 import 금지**, 호출은 상대의 **`api/UseCase` 인터페이스**로만 한다.

서비스를 나누면 상대 도메인이 아예 다른 프로세스(service-bible, service-user)에 있다. 그래서:
1. 상대의 **api 계약 타입(인터페이스 + DTO)만** 내 서비스로 복사한다.
2. 그 인터페이스를 구현하는 **임시 Mock**을 `client/{상대도메인}/...UseCaseMock`에 둔다.
3. 통합 단계에서 Mock을 **RestClient 호출 어댑터**로 갈아끼운다(시그니처가 같으니 교체만 하면 됨).

예: note는 `bible.api.GetBibleVerseUseCase`만 가져오고 `note/client/bible/GetBibleVerseUseCaseMock`으로 임시 구현. 코드는 여전히 `GetBibleVerseUseCase`에 의존하므로, 나중에 Mock→RestClient 교체 시 NoteService는 안 바뀐다.

## 4. ArchUnit — "도메인 무의존"이 아니라 "internal 직접 의존 금지"

service-bible는 `slices().notDependOnEachOther()`(도메인끼리 아예 의존 금지)를 썼다. 하지만 service-note에는 **합법적인 같은-서비스 의존**이 있다: sharing→note.api, report→sharing.api. 이걸 그대로 쓰면 테스트가 실패한다.

그래서 커스텀 규칙으로 바꿨다: **"다른 도메인의 `.internal` 패키지에만 직접 의존하지 마라"**(api/api.dto는 허용).

```java
classes().that().resideInAPackage("com.qtai.domain..")
    .should(다른_도메인_internal에_의존하지_않는다());  // 직접 만든 ArchCondition
```

핵심 아이디어: 클래스의 직접 의존(`getDirectDependenciesFromSelf()`)을 돌면서, 대상 패키지가 `com.qtai.domain.<다른도메인>.internal`이면 위반으로 잡는다. "도메인 이름"은 `com.qtai.domain.` 다음 한 토막으로 뽑는다.

## 5. 트랜잭션 아웃박스 — 이벤트를 잃지 않고 "재처리 가능 상태"로

노트가 바뀌면 알림/집계용 이벤트를 남겨야 한다. 그런데 "노트 저장"과 "이벤트 기록"이 따로 놀면, 둘 사이에 죽었을 때 이벤트가 사라진다.

해결: **같은 트랜잭션 안에서** 이벤트를 PENDING으로 같은 DB에 적재(`JournalEventOutbox`)한다(노트와 함께 커밋/롤백 → 원자성). 실제 처리는 별도 스케줄러(`JournalEventReprocessor`)가 폴링하며 PENDING을 집어 처리한다.

처리에 실패하면 버리지 않고 **FAILED + 다음 재시도 시각(지수 백오프) + retryCount++**로 남긴다. 이게 "유실 0%"라고 단정하지 않고 **"재처리 가능 상태를 남긴다"**고 표현하는 이유다(CLAUDE.md §8 표현 규칙). 실패 로그엔 eventId·eventType·handlerName·errorMessage를 남겨 추적한다.

`@Scheduled`가 실제로 돌려면 부팅 클래스에 **`@EnableScheduling`**이 있어야 한다(빼먹으면 컴파일은 되지만 폴링이 안 됨).

## 6. MockMvc 통합 테스트에서 로그인 흉내내기

컨트롤러는 `@AuthenticationPrincipal Long memberId`로 로그인한 회원 id를 받는다. 테스트에선 `spring-security-test`로 인증 컨텍스트를 주입한다.

```java
authentication(new UsernamePasswordAuthenticationToken(
    memberId, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))))
```

principal에 `memberId`(Long)를 넣으면 `@AuthenticationPrincipal Long`이 그 값으로 해석된다.

- **미인증** 요청은 401 또는 403으로 막힌다. 어느 쪽인지는 시큐리티 엔트리포인트 기본동작에 따라 달라질 수 있어, 테스트는 `401 또는 403`을 모두 허용하도록 단정해 깨지지 않게 했다.
- **denyAll 회귀 방지**: 인증돼 있어도 `/api/v1/admin/**`는 403이어야 한다(검수는 admin-server 소관).

## 7. "제출"과 "검수"를 가르는 기준

신고는 접수(사용자)와 검수(관리자)가 한 도메인에 섞여 있었다. 서비스 분리 기준은 **누가 그 행위를 하느냐**다. 접수는 service-note, 검수는 admin-server. 그래서 `AdminReportService`·`ProcessReportUseCase` 등 검수 코드를 빼고, 검수 상태 전이 메서드(`Report.process()`)도 제거했다.

단, **테이블 컬럼**(processed_by_admin_id 등)은 남긴다 — 단일 DB에서 admin-server가 그 컬럼에 기록하기 때문. "동작(코드)은 분리하되, 데이터(스키마)는 공유"가 단일 DB MSA의 모습이다.
