# JUnit 5 + Spring Test — 테스트 작성법

> **왜 배워야 하나:** QT-AI는 모든 도메인에 테스트 코드가 필수다. 특히 Today QT 캐시, AI 차단 규칙, 도메인 경계 검증 같은 핵심 로직은 테스트 누락을 허용하지 않는다. 노션에서 테스트 프레임워크를 별도로 다루지 않았다.

---

## 1. JUnit 5가 뭔가?

Java에서 테스트 코드를 작성하는 표준 프레임워크다. "이 코드가 올바르게 동작하는가?"를 자동으로 확인한다.

## 2. 기본 테스트 작성

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;

class MemberServiceTest {

    @Test
    @DisplayName("닉네임 변경 후 7일 이내 재변경 시 예외 발생")
    void 닉네임_7일_잠금_테스트() {
        // Given — 준비
        MemberService service = new MemberService();
        service.changeNickname(1L, "첫번째닉네임");

        // When & Then — 실행 + 검증
        assertThatThrownBy(() -> service.changeNickname(1L, "두번째닉네임"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("NICKNAME_CHANGE_LOCKED");
    }
}
```

## 3. 테스트 종류

### 3.1 단위 테스트 (Unit Test)

하나의 클래스/메서드만 테스트한다. 외부 의존성(DB, 네트워크)은 Mock으로 대체한다.

```java
@ExtendWith(MockitoExtension.class)
class QtServiceTest {

    @Mock
    private QtRepository qtRepository;

    @InjectMocks
    private QtService qtService;

    @Test
    void QT_조회_성공() {
        // Mock 설정 — DB 대신 가짜 데이터 반환
        given(qtRepository.findById(1L))
            .willReturn(Optional.of(new Qt(1L, "오늘의 QT")));

        // 실행
        QtResponse result = qtService.getQt(1L);

        // 검증
        assertThat(result.title()).isEqualTo("오늘의 QT");
    }
}
```

### 3.2 슬라이스 테스트 (Slice Test)

특정 레이어만 Spring 컨텍스트를 로드해서 테스트한다:

```java
// Controller 레이어만 테스트
@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @Test
    void 회원_조회_API_200() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
            .andExpect(status().isOk());
    }
}
```

```java
// Repository 레이어만 테스트
@DataJpaTest
class QtRepositoryTest {

    @Autowired
    private QtRepository qtRepository;

    @Test
    void 오늘_QT_조회() {
        Qt qt = qtRepository.findByDate(LocalDate.now());
        assertThat(qt).isNotNull();
    }
}
```

### 3.3 통합 테스트 (Integration Test)

전체 Spring 컨텍스트를 올려서 테스트한다:

```java
@SpringBootTest
@AutoConfigureMockMvc
class MemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 카카오_로그인_전체_흐름() throws Exception {
        mockMvc.perform(post("/api/v1/auth/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accessToken\":\"test-token\"}"))
            .andExpect(status().isOk());
    }
}
```

## 4. 자주 쓰는 검증 메서드 (AssertJ)

```java
assertThat(result).isEqualTo(expected);          // 같은지
assertThat(result).isNotNull();                   // null이 아닌지
assertThat(list).hasSize(3);                      // 리스트 크기
assertThat(list).contains("apple");               // 포함하는지
assertThatThrownBy(() -> service.method())        // 예외 발생하는지
    .isInstanceOf(BusinessException.class);
```

## 5. QT-AI에서 필수 테스트 영역

CLAUDE.md에 명시된 "테스트 누락 불허" 항목:

- 00:00/04:00 Today QT 캐시 동작
- 시뮬레이터 상태와 버튼 활성화 조건
- F-15 Q&A 차단/검증/실패 처리
- 도메인 간 금지 import (ArchUnit)
- admin 권한 검증
- 이벤트 핸들러 실패 로그

## 6. 참고 자료

- JUnit 5 사용자 가이드: https://junit.org/junit5/docs/current/user-guide/
- AssertJ 문서: https://assertj.github.io/doc/
- Spring Boot Testing: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing
