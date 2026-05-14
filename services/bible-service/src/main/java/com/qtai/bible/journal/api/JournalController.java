package com.qtai.bible.journal.api;

import com.qtai.bible.journal.domain.Journal;
import com.qtai.bible.journal.infrastructure.JournalEventRepository;
import com.qtai.bible.journal.infrastructure.JournalRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 묵상 노트 API.
 *
 * <p>경로 (DECISIONS.md §3):
 * - POST  /api/v1/journals/today  — 오늘 QT DRAFT 멱등 생성/조회
 * - GET   /api/v1/journals        — 목록
 * - GET   /api/v1/journals/{id}   — 단건
 * - PATCH /api/v1/journals/{id}   — 4필드 자동 저장 (수정마다 호출)
 * - DELETE /api/v1/journals/{id}  — 삭제
 * - GET   /api/v1/journals/{id}/events — 이벤트 로그
 *
 * <p>금지: 자유 본문 POST /api/v1/journals.
 *
 * <p>TODO(이지윤·이승욱):
 * - PATCH 시 journal.updated 이벤트 append + Kafka 발행 (@TransactionalEventListener AFTER_COMMIT)
 * - DELETE 시 journal.deleted 이벤트 append + soft delete
 * - 본인 소유 검증 (userId == jwt.sub)
 */
@RestController
@RequestMapping("/api/v1/journals")
public class JournalController {

    private final JournalRepository journals;
    private final JournalEventRepository events;

    public JournalController(JournalRepository journals, JournalEventRepository events) {
        this.journals = journals;
        this.events = events;
    }

    /** 오늘 QT DRAFT 멱등 생성/조회. 이미 있으면 그대로 반환. */
    @PostMapping("/today")
    @Transactional
    public ResponseEntity<Journal> createOrGetTodayDraft(@AuthenticationPrincipal Jwt jwt,
                                                         @RequestBody TodayDraftRequest body) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(journals
                .findByUserIdAndQtDate(userId, body.qtDate())
                .orElseGet(() -> journals.save(Journal.newTodayDraft(
                        userId, body.qtDate(), body.bookCode(), body.chapter(), body.verse()))
                        // TODO: journal.created 이벤트 append + AFTER_COMMIT Kafka 발행
                ));
    }

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Long userId = Long.valueOf(jwt.getSubject());
        var pageResult = journals.findAllByUserIdOrderByQtDateDesc(userId, PageRequest.of(page, size));
        return Map.of(
                "items", pageResult.getContent(),
                "page", page,
                "size", size,
                "totalElements", pageResult.getTotalElements()
        );
    }

    @GetMapping("/{id}")
    public Journal get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Journal j = journals.findById(id).orElseThrow(() -> new NoSuchElementException("journal not found"));
        // TODO: 소유자 검증
        return j;
    }

    @PatchMapping("/{id}")
    @Transactional
    public Journal patch(@AuthenticationPrincipal Jwt jwt,
                         @PathVariable Long id,
                         @RequestBody PatchRequest body) {
        Journal j = journals.findById(id).orElseThrow(() -> new NoSuchElementException("journal not found"));
        if (body.felt() != null) j.setFelt(body.felt());
        if (body.memorableVerse() != null) j.setMemorableVerse(body.memorableVerse());
        if (body.application() != null) j.setApplication(body.application());
        if (body.prayer() != null) j.setPrayer(body.prayer());
        // TODO: journal.updated 이벤트 append (sequence + JSON payload) + AFTER_COMMIT Kafka 발행
        return j;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Journal j = journals.findById(id).orElseThrow(() -> new NoSuchElementException("journal not found"));
        journals.delete(j);
        // TODO: journal.deleted 이벤트 append + AFTER_COMMIT Kafka 발행
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/events")
    public Map<String, Object> eventLog(@PathVariable Long id) {
        return Map.of("items", events.findAllByJournalIdOrderBySequenceAsc(id));
    }

    public record TodayDraftRequest(@NotNull LocalDate qtDate,
                                    @NotNull String bookCode,
                                    @NotNull Integer chapter,
                                    @NotNull Integer verse) {}

    public record PatchRequest(String felt,
                               String memorableVerse,
                               String application,
                               String prayer) {}
}
