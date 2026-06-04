package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.qtai.domain.qt.client.sum.SuTodayBibleClient;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(OutputCaptureExtension.class)
class SuTodayPassageImportSchedulerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T15:05:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void disabledSchedulerDoesNotFetchTodayPassage() {
        SuTodayBibleClient client = mock(SuTodayBibleClient.class);
        QtTodayPassageImportService importService = mock(QtTodayPassageImportService.class);
        SuTodayPassageImportScheduler scheduler = scheduler(client, importService, false);

        scheduler.importToday();

        verifyNoInteractions(client);
        verifyNoInteractions(importService);
    }

    @Test
    void enabledSchedulerFetchesAndImportsTodayPassage() {
        SuTodayBibleClient client = mock(SuTodayBibleClient.class);
        QtTodayPassageImportService importService = mock(QtTodayPassageImportService.class);
        QtPassageRepository repository = mock(QtPassageRepository.class);
        SuTodayPassageImportScheduler scheduler = scheduler(client, importService, repository, true);
        SuTodayPassage passage = passage();
        when(client.fetchToday()).thenReturn(passage);
        when(importService.importToday(LocalDate.of(2026, 6, 2), passage)).thenReturn(new QtPassage());

        scheduler.importToday();

        verify(client).fetchToday();
        verify(importService).importToday(LocalDate.of(2026, 6, 2), passage);
    }

    @Test
    void startupImportFetchesOnlyWhenTodayPassageIsMissing() {
        SuTodayBibleClient client = mock(SuTodayBibleClient.class);
        QtTodayPassageImportService importService = mock(QtTodayPassageImportService.class);
        QtPassageRepository repository = mock(QtPassageRepository.class);
        SuTodayPassageImportScheduler scheduler = scheduler(client, importService, repository, true);
        SuTodayPassage passage = passage();
        when(repository.existsByQtDate(LocalDate.of(2026, 6, 2))).thenReturn(false);
        when(client.fetchToday()).thenReturn(passage);
        when(importService.importToday(LocalDate.of(2026, 6, 2), passage)).thenReturn(new QtPassage());

        scheduler.importTodayOnStartup();

        verify(repository).existsByQtDate(LocalDate.of(2026, 6, 2));
        verify(client).fetchToday();
        verify(importService).importToday(LocalDate.of(2026, 6, 2), passage);
    }

    @Test
    void startupImportSkipsWhenTodayPassageAlreadyExists() {
        SuTodayBibleClient client = mock(SuTodayBibleClient.class);
        QtTodayPassageImportService importService = mock(QtTodayPassageImportService.class);
        QtPassageRepository repository = mock(QtPassageRepository.class);
        SuTodayPassageImportScheduler scheduler = scheduler(client, importService, repository, true);
        when(repository.existsByQtDate(LocalDate.of(2026, 6, 2))).thenReturn(true);

        scheduler.importTodayOnStartup();

        verify(repository).existsByQtDate(LocalDate.of(2026, 6, 2));
        verifyNoInteractions(client);
        verifyNoInteractions(importService);
    }

    @Test
    void fetchFailureDoesNotPropagateToScheduler(CapturedOutput output) {
        SuTodayBibleClient client = mock(SuTodayBibleClient.class);
        QtTodayPassageImportService importService = mock(QtTodayPassageImportService.class);
        SuTodayPassageImportScheduler scheduler = scheduler(client, importService, true);
        doThrow(new IllegalStateException("sum unavailable"))
                .when(client)
                .fetchToday();

        assertThatCode(scheduler::importToday)
                .doesNotThrowAnyException();

        verifyNoInteractions(importService);
        assertThat(output).contains(
                "성서유니온 오늘 QT 본문 반영 실패",
                "errorType=IllegalStateException",
                "errorMessage=sum unavailable"
        );
    }

    @Test
    void scheduledTriggerRunsAtFiveMinutesAfterMidnightKst() throws NoSuchMethodException {
        Method importToday = SuTodayPassageImportScheduler.class.getDeclaredMethod("importToday");
        Scheduled scheduled = importToday.getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("0 5 0 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    private static SuTodayPassageImportScheduler scheduler(
            SuTodayBibleClient client,
            QtTodayPassageImportService importService,
            boolean enabled
    ) {
        return scheduler(client, importService, mock(QtPassageRepository.class), enabled);
    }

    private static SuTodayPassageImportScheduler scheduler(
            SuTodayBibleClient client,
            QtTodayPassageImportService importService,
            QtPassageRepository repository,
            boolean enabled
    ) {
        return new SuTodayPassageImportScheduler(client, importService, repository, CLOCK, enabled);
    }

    private static SuTodayPassage passage() {
        return new SuTodayPassage(
                "같은 말, 같은 마음, 같은 뜻",
                "고린도전서",
                "1 Corinthians",
                (short) 1,
                (short) 10,
                (short) 17,
                "고린도전서(1 Corinthians) 1:10-17"
        );
    }
}
