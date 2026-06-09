package com.qtai.domain.qt.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지정 날짜의 QT 콘텐츠를 JSON 스냅샷으로 만들어 오브젝트 스토리지에 올린다(회의록 2026-06-09 §2).
 *
 * <p>본문 조회는 사용자 노출 정책(00:00~04:00 STALE_FALLBACK)을 우회하는 날짜 명시 조회
 * ({@link GetQtPassageContentContextUseCase#findContentContextByDate})를 사용한다 — 내부 배치가
 * "어제 본문"을 스냅샷하는 버그를 막는다(CLAUDE.md §6).
 */
@Service
public class TodayQtSnapshotService {

    private final GetQtPassageContentContextUseCase contentContextUseCase;
    private final QtSnapshotStore snapshotStore;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TodayQtSnapshotService(
            GetQtPassageContentContextUseCase contentContextUseCase,
            QtSnapshotStore snapshotStore,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.contentContextUseCase = contentContextUseCase;
        this.snapshotStore = snapshotStore;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 해당 날짜의 QT 본문이 있으면 스냅샷을 생성·저장하고 저장 위치를 반환한다.
     * 본문이 아직 없으면 {@link Optional#empty()}(배치 미동작 신호 — 관리자 화면에서 점검·수동 생성).
     */
    @Transactional(readOnly = true)
    public Optional<String> exportSnapshot(LocalDate date) {
        return contentContextUseCase.findContentContextByDate(date)
                .map(context -> snapshotStore.store(objectKey(date), serialize(toSnapshot(context))));
    }

    private QtDailySnapshot toSnapshot(QtPassageContentContext context) {
        return new QtDailySnapshot(
                context.qtDate(),
                context.qtPassageId(),
                context.title(),
                context.verseIds(),
                context.published(),
                OffsetDateTime.now(clock).toString()
        );
    }

    private String serialize(QtDailySnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            // 직렬화 실패만 좁게 처리(민감정보·원문 미로깅).
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "QT snapshot serialization failed");
        }
    }

    private static String objectKey(LocalDate date) {
        return date + ".json";
    }
}
