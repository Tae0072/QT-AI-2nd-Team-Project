package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * {@link TodayQtSnapshotService} 단위 테스트.
 *
 * <p>스냅샷은 member-agnostic 콘텐츠(날짜·passage·절 ID·공개여부)만 담아야 하며, 사용자별 값
 * (draftNoteId)·런타임 캐시 상태(cacheStatus)는 포함하지 않는다. 본문이 없으면 저장하지 않는다.
 */
class TodayQtSnapshotServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 9);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final GetQtPassageContentContextUseCase contentContextUseCase =
            mock(GetQtPassageContentContextUseCase.class);
    private final QtSnapshotStore snapshotStore = mock(QtSnapshotStore.class);

    // Spring Boot가 주입하는 ObjectMapper와 동일하게 java.time 모듈 등록 + ISO 문자열 직렬화.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final TodayQtSnapshotService service =
            new TodayQtSnapshotService(contentContextUseCase, snapshotStore, objectMapper, clock);

    @Test
    @DisplayName("해당 날짜 본문이 있으면 'yyyy-MM-dd.json' 키로 member-agnostic JSON을 저장한다")
    void exports_member_agnostic_snapshot_when_passage_exists() {
        QtPassageContentContext context =
                new QtPassageContentContext(101L, DATE, "오늘의 QT", List.of(1001L, 1002L), true);
        when(contentContextUseCase.findContentContextByDate(DATE)).thenReturn(Optional.of(context));
        when(snapshotStore.store(anyString(), anyString())).thenReturn("/snap/2026-06-09.json");

        Optional<String> location = service.exportSnapshot(DATE);

        assertThat(location).contains("/snap/2026-06-09.json");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotStore).store(keyCaptor.capture(), jsonCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("2026-06-09.json");
        String json = jsonCaptor.getValue();
        assertThat(json)
                .contains("\"qtPassageId\":101")
                .contains("\"verseIds\":[1001,1002]")
                .contains("\"published\":true")
                .contains("2026-06-09");
        // 사용자별·런타임 필드가 새어 들어가면 안 된다.
        assertThat(json).doesNotContain("draftNoteId", "cacheStatus", "memberId");
    }

    @Test
    @DisplayName("해당 날짜 본문이 없으면 저장하지 않고 빈 결과를 반환한다(배치 미동작 신호)")
    void does_not_store_when_passage_absent() {
        when(contentContextUseCase.findContentContextByDate(DATE)).thenReturn(Optional.empty());

        Optional<String> location = service.exportSnapshot(DATE);

        assertThat(location).isEmpty();
        verify(snapshotStore, never()).store(anyString(), anyString());
    }

    @Test
    @DisplayName("미공개(published=false) 본문은 정적 URL로 새어 나가지 않도록 저장하지 않는다(§8)")
    void does_not_store_when_passage_unpublished() {
        QtPassageContentContext unpublished =
                new QtPassageContentContext(101L, DATE, "오늘의 QT", List.of(1001L), false);
        when(contentContextUseCase.findContentContextByDate(DATE)).thenReturn(Optional.of(unpublished));

        Optional<String> location = service.exportSnapshot(DATE);

        assertThat(location).isEmpty();
        verify(snapshotStore, never()).store(anyString(), anyString());
    }
}
