package com.qtai.bible;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * QT-AI Bible Service.
 *
 * <p>도메인: 성경 본문(KR/EN), 본문 설명, 해설(구 주석), 묵상 노트(Journal), 익명 나눔.
 * <p>Owner: 이지윤 · 이승욱 (Bible팀) — 추후 김지민 합류 (DECISIONS.md §0)
 * <p>참조: apis/bible/openapi.yaml (2nd-Team-Project)
 *
 * <p>금지 패턴 (AGENTS.md):
 * - 개역개정 / ESV / NIV commit 금지 (저작권)
 * - 독립 journal-service 신규 구현 금지 (Bible 통합)
 * - 자유 본문 POST /api/v1/journals 만들지 않음 — 오늘 QT DRAFT만 POST /api/v1/journals/today
 * - JOURNAL_EVENTS 테이블은 append-only (수정/삭제 금지)
 * - @Transactional 블록 내 KafkaTemplate.send 직접 호출 금지 → @TransactionalEventListener(AFTER_COMMIT)
 *
 * <p>v2.0 Modular Monolith 전환 시 본 모듈은 qtai-server/com.qtai.bible + com.qtai.journal 도메인 패키지로 통합 예정.
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
public class BibleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibleServiceApplication.class, args);
    }
}
