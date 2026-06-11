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

/**
 * 지정 날짜의 QT 콘텐츠를 JSON 스냅샷으로 만들어 오브젝트 스토리지에 올린다(회의록 2026-06-09 §2).
 *
 * <p>본문 조회는 사용자 노출 정책(00:00~04:00 STALE_FALLBACK)을 우회하는 날짜 명시 조회
 * ({@link GetQtPassageContentContextUseCase#findContentContextByDate})를 사용한다 — 내부 배치가
 * "어제 본문"을 스냅샷하는 버그를 막는다(CLAUDE.md §6).
 *
 * <p>노출 통제(§8): <b>published=true(공개)인 본문만</b> 스냅샷한다. 미공개(published=false) 본문이
 * 정적 URL로 새어 나가는 것을 코드·테스트 양쪽에서 차단한다. 스냅샷 생성은 QT 공개 시각(00:00 KST,
 * 콘텐츠가 열리는 시점)에 맞추며, 라이브 API의 04:00 사용자 노출/cache refresh 정책과는 별개 경로다.
 *
 * <p>DB 읽기는 {@code GetQtPassageContentContextUseCase}(자체 트랜잭션) 안에서 끝나고, 외부 스토리지
 * I/O(store)는 트랜잭션 밖에서 수행한다 — 본 서비스에 {@code @Transactional}을 두지 않아 외부 I/O가
 * DB 트랜잭션을 점유하지 않도록 한다(§5).
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
     * 해당 날짜의 <b>공개된(published)</b> QT 본문이 있으면 스냅샷을 생성·저장하고 저장 위치를 반환한다.
     * 본문이 없거나 아직 미공개면 {@link Optional#empty()} — 저장하지 않는다(미공개 본문 정적 노출 차단,
     * 배치 미동작 신호는 관리자 화면에서 점검·수동 생성).
     */
    public Optional<String> exportSnapshot(LocalDate date) {
        return contentContextUseCase.findContentContextByDate(date)
                .filter(QtPassageContentContext::published)
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
