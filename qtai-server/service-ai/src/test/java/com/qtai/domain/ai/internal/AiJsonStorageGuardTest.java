package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link AiJsonStorageGuard} 단위 테스트.
 *
 * <p>CLAUDE.md §7: 외부 AI 응답 원문과 검증용 한국어 주석 원문·참조 자료는 저장/노출하지 않는다.
 * 이 가드는 JSON 직렬화 본문에 금지 필드(provider raw response, validation reference text,
 * commentary original)가 섞여 들어가면 저장을 거부해 정책 위반을 차단한다.
 */
class AiJsonStorageGuardTest {

    @Test
    @DisplayName("null 본문은 그대로 통과한다")
    void null_returns_null() {
        assertThat(AiJsonStorageGuard.rejectRawProviderOrReferenceText(null, "payload")).isNull();
    }

    @Test
    @DisplayName("금지 필드가 없는 정상 JSON은 원본을 그대로 반환한다")
    void clean_json_passes_through() {
        String json = "{\"summary\":\"창세기 1장 해설\",\"verseId\":101}";
        assertThat(AiJsonStorageGuard.rejectRawProviderOrReferenceText(json, "payload")).isEqualTo(json);
    }

    @Test
    @DisplayName("외부 provider 원문 필드가 있으면 저장을 거부한다")
    void rejects_provider_raw_response() {
        String json = "{\"providerRawResponse\":\"<deepseek raw>\"}";
        assertThatThrownBy(() -> AiJsonStorageGuard.rejectRawProviderOrReferenceText(json, "assetPayload"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assetPayload");
    }

    @Test
    @DisplayName("검증 참조 텍스트(snake_case) 필드가 있으면 저장을 거부한다")
    void rejects_validation_reference_text_snake_case() {
        String json = "{ \"validation_reference_text\" : \"성서유니온 주석 원문\" }";
        assertThatThrownBy(() -> AiJsonStorageGuard.rejectRawProviderOrReferenceText(json, "logPayload"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("한국어 주석 원문(commentaryOriginal) 필드가 있으면 저장을 거부한다")
    void rejects_commentary_original() {
        String json = "{\"commentaryOriginal\":\"원문 주석\"}";
        assertThatThrownBy(() -> AiJsonStorageGuard.rejectRawProviderOrReferenceText(json, "payload"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
