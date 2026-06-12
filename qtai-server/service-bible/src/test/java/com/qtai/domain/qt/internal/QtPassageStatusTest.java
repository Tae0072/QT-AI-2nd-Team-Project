package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QtPassageStatusTest {

    @Test
    void apiValueReturnsLowerSnakeCaseContractValue() {
        assertThat(QtPassageStatus.PENDING_REVIEW.apiValue()).isEqualTo("pending_review");
        assertThat(QtPassageStatus.DELETION_NOTIFIED.apiValue()).isEqualTo("deletion_notified");
    }

    @Test
    void fromApiValueAcceptsApiValueAndEnumName() {
        assertThat(QtPassageStatus.fromApiValue("pending_review")).isEqualTo(QtPassageStatus.PENDING_REVIEW);
        assertThat(QtPassageStatus.fromApiValue("PENDING_REVIEW")).isEqualTo(QtPassageStatus.PENDING_REVIEW);
        assertThat(QtPassageStatus.fromApiValue(" active ")).isEqualTo(QtPassageStatus.ACTIVE);
    }

    @Test
    void fromApiValueRejectsBlankOrUnsupportedStatus() {
        assertThatThrownBy(() -> QtPassageStatus.fromApiValue(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status must not be blank");

        assertThatThrownBy(() -> QtPassageStatus.fromApiValue("published"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported status");
    }
}
