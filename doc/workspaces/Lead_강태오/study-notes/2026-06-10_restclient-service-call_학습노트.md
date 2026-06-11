# 학습노트(입문자용) — 서비스끼리 HTTP로 대화하기: Mock → RestClient 어댑터

> 이번에 한 일을 "왜 이렇게 했는지"까지 쉽게 풀어쓴 노트. MSA가 처음이어도 따라올 수 있게 적었다.

## 1. 한 줄 요약

예전에는 노트 서비스가 "성경 구절 정보"를 **가짜(Mock)** 로 지어내서 썼다. 이제는 진짜 **성경 서비스에 HTTP로 물어봐서** 받아온다. 그 다리 역할을 하는 코드가 **RestClient 어댑터**다.

## 2. 왜 Mock이 있었나?

MSA로 쪼개면서 `bible`(성경)과 `note`(노트)가 **서로 다른 서비스(다른 프로세스)** 가 됐다. 노트 코드는 성경 코드를 직접 import 할 수 없다(도메인 경계, CLAUDE.md §4). 그래서 분리 작업 중에는 성경의 "계약(interface)"만 빌려와서, 내용은 **임시로 가짜 구현(Mock)** 해 두고 빌드가 되게 했다.

```
note 서비스 ──(계약: GetBibleVerseUseCase)── ?
                                  ↑ 분리 중엔 Mock이 가짜 데이터로 채움
```

## 3. 이제 진짜로 연결한다 (어댑터)

`GetBibleVerseRestClientAdapter`가 그 계약을 **진짜로** 구현한다. 메서드를 호출하면 성경 서비스의 주소로 HTTP GET을 보낸다.

```
note 서비스 ──HTTP GET /api/v1/bible/verses/by-ids?ids=10,11──▶ bible 서비스
        ◀─────────── {"success":true,"data":[ {구절메타} ]} ──────────
```

핵심 4가지:

1. **주소(base URL)는 설정으로**: `qtai.services.bible-base-url`. 로컬은 `http://localhost:8082`, 배포는 환경변수(`QTAI_SERVICES_BIBLE_BASE_URL`)로 바꾼다. 코드를 안 고치고 주소만 바꿀 수 있게 한 것.
2. **로그인 토큰 전달**: 사용자가 노트를 보려고 보낸 요청에는 JWT(로그인 증표)가 들어있다. 어댑터는 그 `Authorization` 헤더를 **그대로** 성경 서비스에 전달한다. 성경 서비스는 같은 열쇠(공유키)로 토큰을 검사하므로, 유저 서비스에 다시 물어볼 필요가 없다.
3. **에러를 우리 말로 번역**: 성경 서비스가 404(구절 없음)를 주면 우리 공통 에러 `BIBLE_VERSE_NOT_FOUND`로, 서버가 터지면(5xx) `EXTERNAL_API_FAILURE`로 바꾼다. 네트워크 자체가 실패하면 `RestClientException`만 콕 집어 잡는다(아무 예외나 잡는 `catch(Exception)`은 금지 — 진짜 버그를 숨기기 때문).
4. **빈 목록은 호출 안 함**: 물어볼 id가 없으면 네트워크를 타지 않고 바로 빈 결과. 불필요한 호출을 줄인다.

## 4. 받은 JSON 껍데기 벗기기 (ApiResponse)

우리 API는 전부 `{"success":..., "data":..., "error":...}` 모양으로 감싸서 준다(`ApiResponse`). 어댑터는 이 껍데기를 벗겨 `data`만 꺼내 돌려준다. 제네릭이라 `ParameterizedTypeReference`로 "List<BibleVerseResponse> 모양으로 풀어줘"라고 알려줘야 한다.

## 5. 테스트는 어떻게? (진짜 서버 없이)

서비스끼리 통신을 테스트하려고 매번 성경 서버를 띄우면 느리고 불안정하다. 그래서:

- **어댑터 테스트**: `MockRestServiceServer`로 "성경 서버인 척"하는 가짜를 만들어, 정해둔 응답을 돌려준다. URL이 맞는지, 404를 우리 에러로 바꾸는지, 토큰을 전달하는지 검사.
- **엔드포인트 테스트**: 성경 서비스 쪽은 `MockMvc`로 컨트롤러를 직접 두드려 단건 200·다건 200·없으면 404·로그인 안 하면 401을 확인.

## 6. 한 번에 다 안 하는 이유

교체할 Mock이 26개쯤 된다. 한 PR에 다 넣으면 리뷰가 불가능하고 깨지면 원인 찾기 어렵다. 그래서 **호출 쌍 하나씩**(이번엔 note→bible) PR을 쪼갠다. 가장 쉬운 "읽기"부터 시작해 패턴을 만들고, 뒤에 "쓰기·권한" 쌍으로 확장한다.

## 7. 기억할 키워드

`RestClient`(스프링의 동기 HTTP 클라이언트) · `ParameterizedTypeReference`(제네릭 응답 파싱) · `@ConfigurationProperties`(설정을 객체로) · `MockRestServiceServer`(가짜 서버로 테스트) · 도메인 경계(다른 도메인은 `api` 계약으로만).
