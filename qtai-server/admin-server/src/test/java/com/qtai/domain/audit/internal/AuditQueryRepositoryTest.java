package com.qtai.domain.audit.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Arrays;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditQueryRepositoryTest {

    @Autowired
    AuditQueryRepository repository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("findRecent는 dashboard 전용 projection으로 민감 JSON 컬럼을 제외한다")
    void find_recent_uses_dashboard_projection_without_sensitive_json() {
        AuditLog older = AuditLog.create(
                1L,
                "ADMIN",
                7L,
                "operator",
                "AI_ASSET_APPROVE",
                "AI_GENERATED_ASSET",
                500L,
                "{\"prompt\":\"older\"}",
                "{\"payload\":\"older\"}",
                OffsetDateTime.parse("2026-06-10T09:00:00+09:00")
        );
        AuditLog newer = AuditLog.create(
                2L,
                "ADMIN",
                8L,
                "reviewer",
                "REPORT_REVIEW",
                "REPORT",
                600L,
                "{\"prompt\":\"newer\"}",
                "{\"payload\":\"newer\"}",
                OffsetDateTime.parse("2026-06-10T10:00:00+09:00")
        );
        entityManager.persist(older);
        entityManager.persist(newer);
        entityManager.flush();
        entityManager.clear();

        var rows = repository.findRecent(PageRequest.of(0, 1));

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.adminUserId()).isEqualTo(2L);
            assertThat(row.actorType()).isEqualTo("ADMIN");
            assertThat(row.actionType()).isEqualTo("REPORT_REVIEW");
            assertThat(row.targetType()).isEqualTo("REPORT");
            assertThat(row.targetId()).isEqualTo(600L);
            assertThat(row.createdAt()).isEqualTo(OffsetDateTime.parse("2026-06-10T10:00:00+09:00"));
        });
        assertThat(Arrays.stream(AuditQueryRepository.DashboardAuditLogRow.class.getRecordComponents())
                .map(component -> component.getName())
                .toList())
                .doesNotContain("beforeJson", "afterJson", "actorLabel");
    }
}
