package com.qtai.domain.ai.client.qt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.qtai.domain.ai.client.admin.AdminAuthClient;
import com.qtai.domain.ai.client.audit.AuditLogClient;
import com.qtai.domain.ai.client.bible.BibleVerseClient;
import com.qtai.domain.ai.client.qt.dto.QtContextResult;
import com.qtai.domain.ai.client.study.StudyPublishClient;

class AiQtClientContractTest {

    @Test
    void mockAdapterProvidesQtContextThroughAiClientBoundary() {
        QtContextClient client = new GetQtUseCaseMock();

        QtContextResult result = client.getQtContext(10L, 35L);

        assertThat(result.passageId()).isEqualTo(35L);
        assertThat(result.passageReference()).isNotBlank();
        assertThat(result.passageContext()).isNotBlank();
    }

    @Test
    void mockAdapterProvidesTodayQtPassageStatusThroughAiClientBoundary() {
        QtContextClient client = new GetQtUseCaseMock();

        QtContextClient.TodayQtPassageStatus result =
                client.getTodayQtPassageStatus(LocalDate.of(2026, 6, 8));

        assertThat(result.exists()).isTrue();
        assertThat(result.passageId()).isEqualTo(35L);
        assertThat(result.cacheStatus()).isEqualTo(QtContextClient.CacheStatus.HIT);
    }

    @Test
    void todayQtPassageStatusSupportsKnownCacheStatuses() {
        assertThat(QtContextClient.CacheStatus.values())
                .extracting(Enum::name)
                .containsExactly("HIT", "MISS", "STALE_FALLBACK", "EMPTY");
    }

    @Test
    void qtContextResultDoesNotExposeScriptureBodyText() {
        assertThat(QtContextResult.class.getRecordComponents())
                .extracting(component -> component.getName().toLowerCase())
                .doesNotContain("content", "body", "text", "scripturetext", "passagetext");
    }

    @Test
    void aiServiceBoundaryClientsAreInterfaces() {
        assertThat(List.of(
                QtContextClient.class,
                BibleVerseClient.class,
                StudyPublishClient.class,
                AuditLogClient.class,
                AdminAuthClient.class
        )).allSatisfy(type -> assertThat(type).isInterface());
    }
}
