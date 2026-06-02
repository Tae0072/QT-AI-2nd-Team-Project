package com.qtai.domain.audit.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(AuditQueryRepository.class)
class AuditQueryRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private AuditQueryRepository repository;

    @Test
    void findAllAppliesFiltersAndCreatedAtDescIdDescPagination() {
        AuditLog olderMatch = persistLog(
                "ADMIN",
                7L,
                "AI_ASSET_HIDE",
                "AI_GENERATED_ASSET",
                500L,
                "{\"status\":\"APPROVED\"}",
                "{\"status\":\"HIDDEN\"}",
                OffsetDateTime.parse("2026-06-02T00:05:00+09:00")
        );
        AuditLog firstSameTimeMatch = persistLog(
                "ADMIN",
                7L,
                "AI_ASSET_HIDE",
                "AI_GENERATED_ASSET",
                500L,
                "{\"status\":\"VALIDATING\"}",
                "{\"status\":\"HIDDEN\"}",
                OffsetDateTime.parse("2026-06-02T00:06:00+09:00")
        );
        AuditLog secondSameTimeMatch = persistLog(
                "ADMIN",
                7L,
                "AI_ASSET_HIDE",
                "AI_GENERATED_ASSET",
                500L,
                "{\"status\":\"APPROVED\"}",
                "{\"status\":\"HIDDEN\"}",
                OffsetDateTime.parse("2026-06-02T00:06:00+09:00")
        );
        persistLog(
                "ADMIN",
                7L,
                "LOGIN",
                "AI_GENERATED_ASSET",
                500L,
                null,
                "{}",
                OffsetDateTime.parse("2026-06-02T00:07:00+09:00")
        );
        persistLog(
                "ADMIN",
                7L,
                "AI_ASSET_HIDE",
                "MEMBER",
                500L,
                null,
                "{}",
                OffsetDateTime.parse("2026-06-02T00:08:00+09:00")
        );
        persistLog(
                "ADMIN",
                8L,
                "AI_ASSET_HIDE",
                "AI_GENERATED_ASSET",
                500L,
                null,
                "{}",
                OffsetDateTime.parse("2026-06-02T00:09:00+09:00")
        );
        persistLog(
                "ADMIN",
                7L,
                "AI_ASSET_HIDE",
                "AI_GENERATED_ASSET",
                501L,
                null,
                "{}",
                OffsetDateTime.parse("2026-06-02T00:10:00+09:00")
        );
        persistLog(
                "ADMIN",
                7L,
                "AI_ASSET_HIDE",
                "AI_GENERATED_ASSET",
                500L,
                null,
                "{}",
                OffsetDateTime.parse("2026-06-04T00:05:00+09:00")
        );
        flushAndClear();

        AuditQueryRepository.AuditLogPage page = repository.findAll(
                new AuditQueryRepository.Filter(
                        "ADMIN",
                        7L,
                        List.of("AI_ASSET_HIDE"),
                        "AI_GENERATED_ASSET",
                        500L,
                        OffsetDateTime.parse("2026-06-02T00:00:00+09:00"),
                        OffsetDateTime.parse("2026-06-03T00:00:00+09:00")
                ),
                PageRequest.of(0, 2)
        );

        assertThat(page.totalElements()).isEqualTo(3L);
        assertThat(page.content())
                .extracting(AuditQueryRepository.AuditLogRow::id)
                .containsExactly(secondSameTimeMatch.getId(), firstSameTimeMatch.getId());
        assertThat(page.content().get(0).beforeJson()).isEqualTo("{\"status\":\"APPROVED\"}");
        assertThat(page.content().get(0).afterJson()).isEqualTo("{\"status\":\"HIDDEN\"}");

        AuditQueryRepository.AuditLogPage secondPage = repository.findAll(
                new AuditQueryRepository.Filter(
                        "ADMIN",
                        7L,
                        List.of("AI_ASSET_HIDE"),
                        "AI_GENERATED_ASSET",
                        500L,
                        OffsetDateTime.parse("2026-06-02T00:00:00+09:00"),
                        OffsetDateTime.parse("2026-06-03T00:00:00+09:00")
                ),
                PageRequest.of(1, 2)
        );
        assertThat(secondPage.content())
                .extracting(AuditQueryRepository.AuditLogRow::id)
                .containsExactly(olderMatch.getId());
    }

    @Test
    void findAllDefaultsToAiAuditActionScopeWhenActionTypesContainAllowedActions() {
        AuditLog approve = persistLog(
                "ADMIN",
                7L,
                "AI_ASSET_APPROVE",
                "AI_GENERATED_ASSET",
                500L,
                null,
                "{}",
                OffsetDateTime.parse("2026-06-02T00:05:00+09:00")
        );
        AuditLog regenerate = persistLog(
                "ADMIN",
                7L,
                "AI_REGENERATE_REQUEST",
                "AI_GENERATED_ASSET",
                501L,
                null,
                "{}",
                OffsetDateTime.parse("2026-06-02T00:06:00+09:00")
        );
        persistLog(
                "ADMIN",
                7L,
                "CHECKLIST_CREATE",
                "AI_VALIDATION_CHECKLIST_VERSION",
                3L,
                null,
                "{}",
                OffsetDateTime.parse("2026-06-02T00:07:00+09:00")
        );
        flushAndClear();

        AuditQueryRepository.AuditLogPage page = repository.findAll(
                new AuditQueryRepository.Filter(
                        null,
                        null,
                        List.of("AI_ASSET_APPROVE", "AI_ASSET_REJECT", "AI_ASSET_HIDE", "AI_REGENERATE_REQUEST"),
                        "AI_GENERATED_ASSET",
                        null,
                        null,
                        null
                ),
                PageRequest.of(0, 10)
        );

        assertThat(page.totalElements()).isEqualTo(2L);
        assertThat(page.content())
                .extracting(AuditQueryRepository.AuditLogRow::id)
                .containsExactly(regenerate.getId(), approve.getId());
    }

    private AuditLog persistLog(
            String actorType,
            Long actorId,
            String actionType,
            String targetType,
            Long targetId,
            String beforeJson,
            String afterJson,
            OffsetDateTime createdAt
    ) {
        return auditRepository.save(AuditLog.create(
                null,
                actorType,
                actorId,
                actorType + ":" + actorId,
                actionType,
                targetType,
                targetId,
                beforeJson,
                afterJson,
                createdAt
        ));
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }
}
