package com.qtai.domain.qt.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import com.qtai.domain.qt.client.sum.SuTodayPassage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발 전용 — 오늘 날짜 QT 본문이 없으면 더미 본문(시편 23:1-6)을 시딩한다.
 *
 * <p>배경: 성서유니온 자동 수집이 네트워크/HTML 변동으로 실패하거나, 샘플 시드(V8)가
 * 과거 날짜뿐이라 로컬에서 '오늘 QT' 화면이 비어버릴 수 있다. 이 시더는 그 공백을 메운다.
 * (장 교차 본문은 이제 {@code SuTodayPassageParser}가 정상 수집한다)
 *
 * <p>{@link Profile @Profile("local")}로 묶여 운영/스테이징에는 전혀 동작하지 않는다.
 * 본문이 이미 있으면(자동 수집 성공 또는 수동 등록) 아무 일도 하지 않는다.
 * {@link Ordered#LOWEST_PRECEDENCE}로 두어 성서유니온 startup 수집이 끝난 뒤 보강한다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
class LocalTodayQtSeeder {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QtPassageRepository qtPassageRepository;
    private final QtTodayPassageImportService importService;
    private final Clock clock;

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener(ApplicationReadyEvent.class)
    void seedTodayIfMissing() {
        LocalDate today = LocalDate.now(clock.withZone(KST));
        if (qtPassageRepository.existsByQtDate(today)) {
            return;
        }
        try {
            // 시편 23:1-6 — 단일 장이고 bible_verses(V23)에 절이 존재해 절 매핑이 보장된다.
            SuTodayPassage dummy = new SuTodayPassage(
                    "여호와는 나의 목자시니",
                    "시편",
                    "Psalms",
                    (short) 23,
                    (short) 23,
                    (short) 1,
                    (short) 6,
                    "시편 23:1-6"
            );
            QtPassage saved = importService.importToday(today, dummy);
            log.info("[local] 오늘 QT 더미 본문 시딩 완료. qtDate={}, qtPassageId={}", today, saved.getId());
        } catch (RuntimeException e) {
            log.warn("[local] 오늘 QT 더미 본문 시딩 실패. qtDate={}, errorType={}, errorMessage={}",
                    today, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
