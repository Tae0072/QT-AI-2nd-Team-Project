package com.qtai.domain.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.time.OffsetDateTime;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.qtai.domain.ai.client.admin.AdminAuthClient;
import com.qtai.domain.ai.client.admin.AdminAuthClientHttpAdapter;
import com.qtai.domain.ai.client.admin.AdminAuthClientMock;
import com.qtai.domain.ai.client.audit.AuditLogClient;
import com.qtai.domain.ai.client.audit.AuditLogClientHttpAdapter;
import com.qtai.domain.ai.client.audit.AuditLogClientMock;
import com.qtai.domain.ai.client.bible.BibleVerseClient;
import com.qtai.domain.ai.client.bible.BibleVerseClientHttpAdapter;
import com.qtai.domain.ai.client.bible.BibleVerseClientMock;
import com.qtai.domain.ai.client.qt.GetQtUseCaseMock;
import com.qtai.domain.ai.client.qt.QtContextClient;
import com.qtai.domain.ai.client.qt.QtContextClientHttpAdapter;
import com.qtai.domain.ai.client.study.StudyPublishClient;
import com.qtai.domain.ai.client.study.StudyPublishClientHttpAdapter;
import com.qtai.domain.ai.client.study.StudyPublishClientMock;

class AiBoundaryClientContractTest {

    @Test
    void bibleClientMockProvidesVerseAndRangeContracts() {
        BibleVerseClient client = new BibleVerseClientMock();

        BibleVerseClient.BibleVerseResult verse = client.getVerse(16L);
        BibleVerseClient.BibleVerseRangeResult range = client.getVersesInRange("JOHN", 3, 16, 17);

        assertThat(verse.verseId()).isEqualTo(16L);
        assertThat(verse.reference()).isNotBlank();
        assertThat(range.verses()).hasSize(2);
    }

    @Test
    void studyPublishClientMockRecordsPublishAndHideContracts() {
        StudyPublishClientMock client = new StudyPublishClientMock();
        OffsetDateTime approvedAt = OffsetDateTime.parse("2026-06-08T00:00:00+09:00");

        client.publishApprovedVerseExplanation(new StudyPublishClient.PublishVerseExplanationCommand(
                16L,
                "summary",
                "explanation",
                "QT-AI",
                100L,
                approvedAt
        ));
        client.hidePublishedVerseExplanation(new StudyPublishClient.HideVerseExplanationCommand(100L));

        assertThat(client.publishedCommands()).hasSize(1);
        assertThat(client.hiddenCommands()).hasSize(1);
    }

    @Test
    void auditLogClientMockRecordsAuditCommandContract() {
        AuditLogClientMock client = new AuditLogClientMock();

        client.writeAuditLog(new AuditLogClient.AuditLogCommand(
                1L,
                "ADMIN",
                10L,
                "ADMIN:10",
                "AI_ASSET_APPROVE",
                "AI_GENERATED_ASSET",
                100L,
                "{}",
                "{}"
        ));

        assertThat(client.writtenCommands()).singleElement()
                .extracting(AuditLogClient.AuditLogCommand::actionType)
                .isEqualTo("AI_ASSET_APPROVE");
    }

    @Test
    void adminAuthClientMockUsesTypedAdminRoleContract() {
        AdminAuthClient client = new AdminAuthClientMock();

        AdminAuthClient.AdminAuthResult result =
                client.verifyAnyRole(10L, List.of(AdminAuthClient.AdminRole.REVIEWER));

        assertThat(result.memberId()).isEqualTo(10L);
        assertThat(result.adminRole()).isEqualTo(AdminAuthClient.AdminRole.REVIEWER);
    }

    @Test
    void bibleClientMockRejectsInvalidVerseInputsWithValidationFailure() {
        BibleVerseClient client = new BibleVerseClientMock();

        assertValidationFailure(() -> client.getVerse(null), "bible");
        assertValidationFailure(() -> client.getVersesByIds(null), "bible");
        assertValidationFailure(() -> client.getVersesByIds(List.of()), "bible");
        assertValidationFailure(() -> client.getVersesByIds(Arrays.asList(16L, null)), "bible");
    }

    @Test
    void adminAuthClientMockRejectsInvalidRoleInputsWithValidationFailure() {
        AdminAuthClient client = new AdminAuthClientMock();

        assertValidationFailure(() -> client.verifyAnyRole(10L, null), "admin-auth");
        assertValidationFailure(() -> client.verifyAnyRole(10L, List.of()), "admin-auth");
        assertValidationFailure(
                () -> client.verifyAnyRole(10L, Arrays.asList(AdminAuthClient.AdminRole.REVIEWER, null)),
                "admin-auth"
        );
    }

    @Test
    void boundaryClientMocksAreGuardedAgainstProductionRegistration() {
        assertMockRegistrationGuard(GetQtUseCaseMock.class, QtContextClient.class);
        assertMockRegistrationGuard(BibleVerseClientMock.class, BibleVerseClient.class);
        assertMockRegistrationGuard(StudyPublishClientMock.class, StudyPublishClient.class);
        assertMockRegistrationGuard(AuditLogClientMock.class, AuditLogClient.class);
        assertMockRegistrationGuard(AdminAuthClientMock.class, AdminAuthClient.class);
    }

    @Test
    void boundaryClientHttpAdaptersAreGuardedByHttpMode() {
        assertHttpAdapterRegistrationGuard(QtContextClientHttpAdapter.class, QtContextClient.class);
        assertHttpAdapterRegistrationGuard(BibleVerseClientHttpAdapter.class, BibleVerseClient.class);
        assertHttpAdapterRegistrationGuard(StudyPublishClientHttpAdapter.class, StudyPublishClient.class);
        assertHttpAdapterRegistrationGuard(AuditLogClientHttpAdapter.class, AuditLogClient.class);
        assertHttpAdapterRegistrationGuard(AdminAuthClientHttpAdapter.class, AdminAuthClient.class);
    }

    @Test
    void boundaryClientMethodsDeclareSharedFailureModel() {
        List.of(
                QtContextClient.class,
                BibleVerseClient.class,
                StudyPublishClient.class,
                AuditLogClient.class,
                AdminAuthClient.class
        ).forEach(AiBoundaryClientContractTest::assertClientFailureModel);

        assertThat(AiClientException.FailureCode.TIMEOUT.retryable()).isTrue();
        assertThat(AiClientException.FailureCode.CIRCUIT_OPEN.retryable()).isTrue();
        assertThat(AiClientException.FailureCode.UNAUTHORIZED.retryable()).isFalse();
        assertThat(AiClientException.FailureCode.VALIDATION_FAILED.retryable()).isFalse();
    }

    private static void assertMockRegistrationGuard(Class<?> mockType, Class<?> clientType) {
        assertThat(mockType.getAnnotation(Component.class)).isNotNull();
        assertThat(mockType.getAnnotation(Primary.class)).isNull();

        Profile profile = mockType.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).contains("local", "test");

        ConditionalOnProperty property = mockType.getAnnotation(ConditionalOnProperty.class);
        assertThat(property).isNotNull();
        assertThat(property.name()).contains("qtai.ai.client.mode");
        assertThat(property.havingValue()).isEqualTo("mock");
        assertThat(property.matchIfMissing()).isTrue();

        ConditionalOnMissingBean missingBean = mockType.getAnnotation(ConditionalOnMissingBean.class);
        assertThat(missingBean).isNotNull();
        assertThat(missingBean.value()).contains(clientType);
    }

    private static void assertHttpAdapterRegistrationGuard(Class<?> adapterType, Class<?> clientType) {
        assertThat(adapterType.getAnnotation(Component.class)).isNotNull();
        assertThat(adapterType.getAnnotation(Primary.class)).isNull();
        assertThat(adapterType.getAnnotation(Profile.class)).isNull();

        ConditionalOnProperty property = adapterType.getAnnotation(ConditionalOnProperty.class);
        assertThat(property).isNotNull();
        assertThat(property.name()).contains("qtai.ai.client.mode");
        assertThat(property.havingValue()).isEqualTo("http");
        assertThat(property.matchIfMissing()).isFalse();

        ConditionalOnMissingBean missingBean = adapterType.getAnnotation(ConditionalOnMissingBean.class);
        assertThat(missingBean).isNotNull();
        assertThat(missingBean.value()).contains(clientType);
    }

    private static void assertClientFailureModel(Class<?> clientType) {
        assertThat(clientType.getDeclaredMethods())
                .allSatisfy(method -> assertThat(Arrays.asList(method.getExceptionTypes()))
                        .contains(AiClientException.class));
    }

    private static void assertValidationFailure(ThrowingCallable callable, String downstreamService) {
        assertThatThrownBy(callable)
                .isInstanceOf(AiClientException.class)
                .satisfies(exception -> {
                    AiClientException aiClientException = (AiClientException) exception;
                    assertThat(aiClientException.failureCode())
                            .isEqualTo(AiClientException.FailureCode.VALIDATION_FAILED);
                    assertThat(aiClientException.downstreamService()).isEqualTo(downstreamService);
                    assertThat(aiClientException.retryable()).isFalse();
                });
    }
}
