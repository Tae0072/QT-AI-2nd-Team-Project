package com.qtai.domain.qt.api;

import java.time.LocalDate;
import java.util.Optional;

import com.qtai.domain.qt.api.dto.QtPassageContentContext;

public interface GetQtPassageContentContextUseCase {

    QtPassageContentContext getContentContext(Long qtPassageId);

    /**
     * 특정 날짜의 QT 본문 콘텐츠 컨텍스트를 조회한다 — 내부 배치 전용.
     *
     * <p>사용자용 {@code GetTodayQtUseCase.getToday}는 00:00~04:00에 전일 본문을
     * 반환하는 노출 정책(STALE_FALLBACK)이 섞여 있어, 00:05 해설 시딩 같은
     * 내부 배치가 그대로 쓰면 "어제 본문"을 시딩하는 버그가 된다.
     * 내부 배치는 이 메서드로 노출 정책을 우회해 날짜를 명시 조회한다 (CLAUDE.md §6).
     *
     * @param qtDate 조회할 QT 날짜 (KST 기준)
     * @return 해당 날짜 본문이 없으면 {@link Optional#empty()}
     */
    Optional<QtPassageContentContext> findContentContextByDate(LocalDate qtDate);
}
