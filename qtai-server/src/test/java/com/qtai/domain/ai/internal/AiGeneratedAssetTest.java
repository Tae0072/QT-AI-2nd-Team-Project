package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class AiGeneratedAssetTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-20T04:03:00+09:00");
    private static final OffsetDateTime REVIEWED_AT = OffsetDateTime.parse("2026-05-20T04:04:00+09:00");

    @Test
    void approvedAssetCannotBeRejected() {
        AiGeneratedAsset asset = newAsset();
        asset.approve(REVIEWED_AT);

        assertThatThrownBy(() -> asset.reject(REVIEWED_AT.plusMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.APPROVED);
    }

    @Test
    void rejectedAssetCannotBeApproved() {
        AiGeneratedAsset asset = newAsset();
        asset.reject(REVIEWED_AT);

        assertThatThrownBy(() -> asset.approve(REVIEWED_AT.plusMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
    }

    @Test
    void approvedAssetCanBeHidden() {
        AiGeneratedAsset asset = newAsset();
        asset.approve(REVIEWED_AT);

        asset.hide(REVIEWED_AT.plusMinutes(1));

        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.HIDDEN);
        assertThat(asset.getReviewedAt()).isEqualTo(REVIEWED_AT.plusMinutes(1));
    }

    @Test
    void blankPayloadJsonIsRejected() {
        assertThatThrownBy(() -> AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                "2026.05.1",
                " ",
                "QT-AI verified content",
                CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payloadJson must not be blank");
    }

    @Test
    void blankPromptVersionIsRejected() {
        assertThatThrownBy(() -> AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                " ",
                "{\"summary\":\"validated\"}",
                "QT-AI verified content",
                CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("promptVersion must not be blank");
    }

    @Test
    void payloadJsonCannotStoreProviderRawResponse() {
        assertThatThrownBy(() -> AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                "2026.05.1",
                "{\"providerRawResponse\":\"full model output\"}",
                "QT-AI verified content",
                CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payloadJson");
    }

    private static AiGeneratedAsset newAsset() {
        return AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                "2026.05.1",
                "{\"summary\":\"validated\"}",
                "QT-AI verified content",
                CREATED_AT
        );
    }
}
