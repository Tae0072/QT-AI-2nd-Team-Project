package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class RestrictedStorageUriResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void restrictedUriIsResolvedUnderConfiguredRoot() {
        RestrictedStorageUriResolver resolver = new RestrictedStorageUriResolver(tempDir);

        Path resolved = resolver.resolve("restricted://validation/index/reference-index.json");

        assertThat(resolved).isEqualTo(tempDir.resolve("validation").resolve("index").resolve("reference-index.json"));
    }

    @Test
    void blankUriIsRejected() {
        RestrictedStorageUriResolver resolver = new RestrictedStorageUriResolver(tempDir);

        assertThatThrownBy(() -> resolver.resolve(" "))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessage("AI_REVIEW_REFERENCE_INDEX_URI_INVALID");
    }

    @Test
    void nonRestrictedSchemeIsRejected() {
        RestrictedStorageUriResolver resolver = new RestrictedStorageUriResolver(tempDir);

        assertThatThrownBy(() -> resolver.resolve("file:///validation/index/reference-index.json"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessage("AI_REVIEW_REFERENCE_INDEX_URI_INVALID");
    }

    @Test
    void pathTraversalIsRejected() {
        RestrictedStorageUriResolver resolver = new RestrictedStorageUriResolver(tempDir);

        assertThatThrownBy(() -> resolver.resolve("restricted://validation/../secret.json"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessage("AI_REVIEW_REFERENCE_INDEX_URI_INVALID");
    }
}
