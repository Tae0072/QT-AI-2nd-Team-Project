package com.qtai.domain.ai.client.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.qtai.config.JacksonConfig;
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

class AiHttpClientRuntimeToggleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RuntimeToggleTestConfig.class);

    @Test
    void testProfileWithMissingModeRegistersMocksOnly() {
        withProfile("test").run(context -> {
            assertThat(context).hasNotFailed();
            assertMockClients(context);
            assertNoHttpAdapters(context);
        });
    }

    @Test
    void testProfileWithMockModeRegistersMocksOnly() {
        withProfile("test")
                .withPropertyValues("qtai.ai.client.mode=mock")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertMockClients(context);
                    assertNoHttpAdapters(context);
                });
    }

    @Test
    void testProfileWithHttpModeRegistersHttpAdaptersOnly() {
        withProfile("test")
                .withPropertyValues(httpModeProperties())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertHttpClients(context);
                    assertNoMockClients(context);
                });
    }

    @Test
    void prodProfileWithMissingModeDoesNotRegisterMocksOrHttpAdapters() {
        withProfile("prod").run(context -> {
            assertThat(context).hasNotFailed();
            assertNoClientInterfaces(context);
            assertNoMockClients(context);
            assertNoHttpAdapters(context);
        });
    }

    @Test
    void httpModeFailsFastWhenServiceTokenIsMissing() {
        withProfile("test")
                .withPropertyValues(httpModePropertiesExcept("qtai.ai.client.service-token"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("service-token");
                });
    }

    @Test
    void httpModeFailsFastWhenBaseUrlIsMissing() {
        withProfile("test")
                .withPropertyValues(httpModePropertiesExcept("qtai.ai.client.bible.base-url"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("bible base-url");
                });
    }

    private ApplicationContextRunner withProfile(String profile) {
        return contextRunner.withPropertyValues("spring.profiles.active=" + profile);
    }

    private static String[] httpModeProperties() {
        return new String[] {
                "qtai.ai.client.mode=http",
                "qtai.ai.client.service-token=test-service-token",
                "qtai.ai.client.qt.base-url=http://localhost:65530",
                "qtai.ai.client.bible.base-url=http://localhost:65531",
                "qtai.ai.client.study.base-url=http://localhost:65532",
                "qtai.ai.client.audit.base-url=http://localhost:65533",
                "qtai.ai.client.admin-auth.base-url=http://localhost:65534"
        };
    }

    private static String[] httpModePropertiesExcept(String excludedPropertyName) {
        return java.util.Arrays.stream(httpModeProperties())
                .filter(property -> !property.startsWith(excludedPropertyName + "="))
                .toArray(String[]::new);
    }

    private static void assertMockClients(AssertableApplicationContext context) {
        assertThat(context).hasSingleBean(QtContextClient.class);
        assertThat(context.getBean(QtContextClient.class)).isInstanceOf(GetQtUseCaseMock.class);
        assertThat(context).hasSingleBean(BibleVerseClient.class);
        assertThat(context.getBean(BibleVerseClient.class)).isInstanceOf(BibleVerseClientMock.class);
        assertThat(context).hasSingleBean(StudyPublishClient.class);
        assertThat(context.getBean(StudyPublishClient.class)).isInstanceOf(StudyPublishClientMock.class);
        assertThat(context).hasSingleBean(AuditLogClient.class);
        assertThat(context.getBean(AuditLogClient.class)).isInstanceOf(AuditLogClientMock.class);
        assertThat(context).hasSingleBean(AdminAuthClient.class);
        assertThat(context.getBean(AdminAuthClient.class)).isInstanceOf(AdminAuthClientMock.class);
    }

    private static void assertHttpClients(AssertableApplicationContext context) {
        assertThat(context).hasSingleBean(QtContextClient.class);
        assertThat(context.getBean(QtContextClient.class)).isInstanceOf(QtContextClientHttpAdapter.class);
        assertThat(context).hasSingleBean(BibleVerseClient.class);
        assertThat(context.getBean(BibleVerseClient.class)).isInstanceOf(BibleVerseClientHttpAdapter.class);
        assertThat(context).hasSingleBean(StudyPublishClient.class);
        assertThat(context.getBean(StudyPublishClient.class)).isInstanceOf(StudyPublishClientHttpAdapter.class);
        assertThat(context).hasSingleBean(AuditLogClient.class);
        assertThat(context.getBean(AuditLogClient.class)).isInstanceOf(AuditLogClientHttpAdapter.class);
        assertThat(context).hasSingleBean(AdminAuthClient.class);
        assertThat(context.getBean(AdminAuthClient.class)).isInstanceOf(AdminAuthClientHttpAdapter.class);
    }

    private static void assertNoClientInterfaces(AssertableApplicationContext context) {
        assertThat(context).doesNotHaveBean(QtContextClient.class);
        assertThat(context).doesNotHaveBean(BibleVerseClient.class);
        assertThat(context).doesNotHaveBean(StudyPublishClient.class);
        assertThat(context).doesNotHaveBean(AuditLogClient.class);
        assertThat(context).doesNotHaveBean(AdminAuthClient.class);
    }

    private static void assertNoMockClients(AssertableApplicationContext context) {
        assertThat(context).doesNotHaveBean(GetQtUseCaseMock.class);
        assertThat(context).doesNotHaveBean(BibleVerseClientMock.class);
        assertThat(context).doesNotHaveBean(StudyPublishClientMock.class);
        assertThat(context).doesNotHaveBean(AuditLogClientMock.class);
        assertThat(context).doesNotHaveBean(AdminAuthClientMock.class);
    }

    private static void assertNoHttpAdapters(AssertableApplicationContext context) {
        assertThat(context).doesNotHaveBean(QtContextClientHttpAdapter.class);
        assertThat(context).doesNotHaveBean(BibleVerseClientHttpAdapter.class);
        assertThat(context).doesNotHaveBean(StudyPublishClientHttpAdapter.class);
        assertThat(context).doesNotHaveBean(AuditLogClientHttpAdapter.class);
        assertThat(context).doesNotHaveBean(AdminAuthClientHttpAdapter.class);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    @Configuration
    @Import({
            AiClientConfiguration.class,
            JacksonConfig.class,
            GetQtUseCaseMock.class,
            BibleVerseClientMock.class,
            StudyPublishClientMock.class,
            AuditLogClientMock.class,
            AdminAuthClientMock.class,
            QtContextClientHttpAdapter.class,
            BibleVerseClientHttpAdapter.class,
            StudyPublishClientHttpAdapter.class,
            AuditLogClientHttpAdapter.class,
            AdminAuthClientHttpAdapter.class
    })
    static class RuntimeToggleTestConfig {
    }
}
