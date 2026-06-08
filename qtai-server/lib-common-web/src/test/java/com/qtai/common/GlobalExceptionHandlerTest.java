package com.qtai.common;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler 테스트.
 *
 * 대부분은 standalone MockMvc 환경에서 검증.
 * 단, MethodArgumentTypeMismatchException은 standalone setup에서 Spring MVC ExceptionResolver
 * 라우팅이 우리 핸들러까지 도달하지 못하는 한계가 있어 핸들러 메서드를 직접 호출(unit) 방식으로 검증한다.
 * 통합 환경에서의 실제 동작은 NoteRepositoryIntegrationTest / Postman 수동 검증 또는 다음 PR 통합 테스트에서 확인.
 */
@SuppressWarnings("null") // Eclipse JDT가 assertThat(body).isNotNull() 후의 null 안전을 추론 못함. 빌드·실행 영향 없음.
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ErrorTestController())
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    @DisplayName("BusinessException → 해당 ErrorCode의 HTTP 상태 + 표준 에러 본문")
    void businessException_returns_errorCode_status() throws Exception {
        mockMvc.perform(get("/test/business-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0001"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("예상치 못한 Exception → 500 + C0001 에러 코드")
    void unexpectedException_returns_500() throws Exception {
        mockMvc.perform(get("/test/unexpected-error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0001"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 + C0002 에러 코드")
    void validationException_returns_400() throws Exception {
        mockMvc.perform(post("/test/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0002"));
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException (enum 잘못된 값) → 400 + C0002 + 허용 값 목록 메시지")
    void typeMismatch_enum_returns_400_with_allowed_values() {
        // given — enum 변환 실패를 흉내내는 mock 예외
        MethodArgumentTypeMismatchException e = mock(MethodArgumentTypeMismatchException.class);
        when(e.getName()).thenReturn("kind");
        when(e.getValue()).thenReturn("INVALID");
        doReturn(SampleEnum.class).when(e).getRequiredType();

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.error()).isNotNull();
        assertThat(body.error().code()).isEqualTo("C0002");
        assertThat(body.error().message())
                .contains("kind")
                .contains("INVALID")
                .contains("허용 값: ALPHA, BETA");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException (Integer 자리에 문자열) → 400 + C0002 + 형식 메시지")
    void typeMismatch_integer_returns_400_with_type_message() {
        // given
        MethodArgumentTypeMismatchException e = mock(MethodArgumentTypeMismatchException.class);
        when(e.getName()).thenReturn("number");
        when(e.getValue()).thenReturn("abc");
        doReturn(Integer.class).when(e).getRequiredType();

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.error().code()).isEqualTo("C0002");
        assertThat(body.error().message())
                .contains("number")
                .contains("abc")
                .contains("Integer 형식이 아닙니다");
    }

    @Test
    @DisplayName("DataIntegrityViolationException(동시성 UNIQUE 위반) → 409 + C0003 (기존 500 누출 방지)")
    void dataIntegrityViolation_returns_409() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(
                new org.springframework.dao.DataIntegrityViolationException("Duplicate entry for key uk_..."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error().code()).isEqualTo("C0003");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException(잘못된 JSON) → 400 + C0002 (기존 500 방지)")
    void notReadable_returns_400() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotReadable(
                new org.springframework.http.converter.HttpMessageNotReadableException(
                        "JSON parse error",
                        new org.springframework.mock.http.MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("C0002");
    }

    @Test
    @DisplayName("AccessDeniedException(@PreAuthorize 거부) → 403 + M0003 (기존 500 방지)")
    void accessDenied_returns_403() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(
                new org.springframework.security.access.AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("M0003");
    }

    @Test
    @DisplayName("ConstraintViolationException(@RequestParam 검증) → 400 + C0002 (기존 500 방지)")
    void constraintViolation_returns_400() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(
                new jakarta.validation.ConstraintViolationException("page: 0 이상", java.util.Set.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("C0002");
    }

    // ── 테스트용 더미 컨트롤러 ──

    @RestController
    static class ErrorTestController {

        @GetMapping("/test/business-error")
        public void throwBusiness() {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        @GetMapping("/test/unexpected-error")
        public void throwUnexpected() {
            throw new RuntimeException("테스트용 예상치 못한 예외");
        }

        @PostMapping("/test/validation-error")
        public void throwValidation(@Valid @RequestBody ValidationDto dto) {
            // @Valid가 MethodArgumentNotValidException을 발생시킴
        }
    }

    @Getter @Setter
    static class ValidationDto {
        @NotBlank
        private String name;
    }

    enum SampleEnum { ALPHA, BETA }
}
