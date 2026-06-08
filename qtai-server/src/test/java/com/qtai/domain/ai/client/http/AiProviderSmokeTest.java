package com.qtai.domain.ai.client.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qtai.domain.ai.client.admin.AdminAuthClient;
import com.qtai.domain.ai.client.admin.AdminAuthClient.AdminAuthResult;
import com.qtai.domain.ai.client.admin.AdminAuthClient.AdminRole;
import com.qtai.domain.ai.client.admin.AdminAuthClientHttpAdapter;
import com.qtai.domain.ai.client.bible.BibleVerseClient;
import com.qtai.domain.ai.client.bible.BibleVerseClient.BibleVerseRangeResult;
import com.qtai.domain.ai.client.bible.BibleVerseClient.BibleVerseResult;
import com.qtai.domain.ai.client.bible.BibleVerseClientHttpAdapter;
import com.qtai.domain.ai.client.qt.QtContextClient;
import com.qtai.domain.ai.client.qt.QtContextClient.TodayQtPassageStatus;
import com.qtai.domain.ai.client.qt.QtContextClientHttpAdapter;
import com.qtai.domain.ai.client.qt.dto.QtContextResult;

@Tag("provider-smoke")
@EnabledIfEnvironmentVariable(named = "QTAI_PROVIDER_SMOKE_ENABLED", matches = "true")
class AiProviderSmokeTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void qtReadEndpointsRespondThroughHttpAdapter() {
        SmokeSettings settings = SmokeSettings.fromEnvironment();
        QtContextClient client = new QtContextClientHttpAdapter(objectMapper, properties(settings));

        QtContextResult context = client.getQtContext(0L, settings.qtPassageId());
        TodayQtPassageStatus todayStatus = client.getTodayQtPassageStatus(settings.qtDate());

        assertThat(context.passageId()).isEqualTo(settings.qtPassageId());
        assertThat(context.passageReference()).isNotBlank();
        assertThat(context.passageContext()).isNotBlank();
        assertThat(todayStatus.qtDate()).isEqualTo(settings.qtDate());
        assertThat(todayStatus.cacheStatus()).isNotNull();
        if (todayStatus.exists()) {
            assertThat(todayStatus.passageId()).isNotNull();
        }
    }

    @Test
    void bibleReadEndpointsRespondThroughHttpAdapter() {
        SmokeSettings settings = SmokeSettings.fromEnvironment();
        BibleVerseClient client = new BibleVerseClientHttpAdapter(objectMapper, properties(settings));

        BibleVerseResult single = client.getVerse(settings.bibleVerseId());
        List<BibleVerseResult> batch = client.getVersesByIds(settings.bibleBatchVerseIds());
        BibleVerseRangeResult range = client.getVersesInRange(
                settings.bibleBook(),
                settings.bibleChapter(),
                settings.bibleStartVerse(),
                settings.bibleEndVerse()
        );

        assertThat(single.verseId()).isEqualTo(settings.bibleVerseId());
        assertThat(single.reference()).isNotBlank();
        assertThat(single.koreanText()).isNotBlank();
        assertThat(single.englishText()).isNotBlank();
        assertThat(batch.stream().map(BibleVerseResult::verseId).toList())
                .containsAll(settings.bibleBatchVerseIds());
        assertThat(range.bibleBook()).isEqualTo(settings.bibleBook());
        assertThat(range.chapter()).isEqualTo(settings.bibleChapter());
        assertThat(range.verses()).isNotEmpty();
    }

    @Test
    void adminAuthReadEndpointsRespondThroughHttpAdapter() {
        SmokeSettings settings = SmokeSettings.fromEnvironment();
        AdminAuthClient client = new AdminAuthClientHttpAdapter(objectMapper, properties(settings));

        AdminAuthResult active = client.getActiveAdmin(settings.adminMemberId());
        AdminAuthResult verified = client.verifyRole(settings.adminMemberId(), settings.adminRole());
        AdminAuthResult verifiedAny = client.verifyAnyRole(settings.adminMemberId(), settings.adminRoles());

        assertThat(active.memberId()).isEqualTo(settings.adminMemberId());
        assertThat(active.adminUserId()).isNotNull();
        assertThat(active.adminRole()).isNotNull();
        assertThat(verified.memberId()).isEqualTo(settings.adminMemberId());
        assertThat(verified.adminRole()).isNotNull();
        assertThat(verifiedAny.memberId()).isEqualTo(settings.adminMemberId());
        assertThat(verifiedAny.adminRole()).isNotNull();
    }

    private static AiClientProperties properties(SmokeSettings settings) {
        AiClientProperties properties = new AiClientProperties();
        properties.setMode(AiClientProperties.Mode.HTTP);
        properties.setServiceToken(settings.serviceToken());
        properties.setTimeoutMs(settings.timeoutMs());
        properties.getQt().setBaseUrl(settings.qtBaseUrl());
        properties.getBible().setBaseUrl(settings.bibleBaseUrl());
        properties.getAdminAuth().setBaseUrl(settings.adminAuthBaseUrl());
        return properties;
    }

    private record SmokeSettings(
            String serviceToken,
            int timeoutMs,
            String qtBaseUrl,
            String bibleBaseUrl,
            String adminAuthBaseUrl,
            Long qtPassageId,
            LocalDate qtDate,
            Long bibleVerseId,
            List<Long> bibleBatchVerseIds,
            String bibleBook,
            int bibleChapter,
            Integer bibleStartVerse,
            Integer bibleEndVerse,
            Long adminMemberId,
            AdminRole adminRole,
            List<AdminRole> adminRoles
    ) {

        private static SmokeSettings fromEnvironment() {
            return new SmokeSettings(
                    requiredEnv("QTAI_AI_CLIENT_SERVICE_TOKEN"),
                    positiveIntEnv("QTAI_PROVIDER_SMOKE_TIMEOUT_MS", 3000),
                    requiredEnv("QTAI_AI_CLIENT_QT_BASE_URL"),
                    requiredEnv("QTAI_AI_CLIENT_BIBLE_BASE_URL"),
                    requiredEnv("QTAI_AI_CLIENT_ADMIN_AUTH_BASE_URL"),
                    longEnv("QTAI_PROVIDER_SMOKE_QT_PASSAGE_ID"),
                    dateEnv("QTAI_PROVIDER_SMOKE_QT_DATE"),
                    longEnv("QTAI_PROVIDER_SMOKE_BIBLE_VERSE_ID"),
                    longListEnv("QTAI_PROVIDER_SMOKE_BIBLE_BATCH_VERSE_IDS"),
                    requiredEnv("QTAI_PROVIDER_SMOKE_BIBLE_BOOK"),
                    intEnv("QTAI_PROVIDER_SMOKE_BIBLE_CHAPTER"),
                    intEnv("QTAI_PROVIDER_SMOKE_BIBLE_START_VERSE"),
                    intEnv("QTAI_PROVIDER_SMOKE_BIBLE_END_VERSE"),
                    longEnv("QTAI_PROVIDER_SMOKE_ADMIN_MEMBER_ID"),
                    roleEnv("QTAI_PROVIDER_SMOKE_ADMIN_ROLE"),
                    roleListEnv("QTAI_PROVIDER_SMOKE_ADMIN_ROLES")
            );
        }

        private static String requiredEnv(String name) {
            String value = System.getenv(name);
            assertThat(value)
                    .as("environment variable %s must be set for provider smoke test", name)
                    .isNotBlank();
            return value.trim();
        }

        private static Long longEnv(String name) {
            return Long.valueOf(requiredEnv(name));
        }

        private static int intEnv(String name) {
            return Integer.parseInt(requiredEnv(name));
        }

        private static int positiveIntEnv(String name, int defaultValue) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            int parsed = Integer.parseInt(value.trim());
            assertThat(parsed)
                    .as("environment variable %s must be positive", name)
                    .isPositive();
            return parsed;
        }

        private static LocalDate dateEnv(String name) {
            return LocalDate.parse(requiredEnv(name));
        }

        private static List<Long> longListEnv(String name) {
            return Arrays.stream(requiredEnv(name).split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::valueOf)
                    .toList();
        }

        private static AdminRole roleEnv(String name) {
            return AdminRole.valueOf(requiredEnv(name));
        }

        private static List<AdminRole> roleListEnv(String name) {
            return Arrays.stream(requiredEnv(name).split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(AdminRole::valueOf)
                    .toList();
        }
    }
}
