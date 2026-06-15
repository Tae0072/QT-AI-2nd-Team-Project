package com.qtai.domain.audit.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Test
    @DisplayName("findAll은 targetType=null이면 대상 무관으로 AI 액션을 조회해 QT_PASSAGE 해설 생성 트리거도 노출한다")
    void find_all_with_null_target_type_includes_qt_passage_explanation_generate() {
        AuditLog assetApprove = AuditLog.create(
                1L, "ADMIN", 7L, "ADMIN:7",
                "AI_ASSET_APPROVE", "AI_GENERATED_ASSET", 500L,
                "{}", "{}", OffsetDateTime.parse("2026-06-15T09:00:00+09:00"));
        AuditLog explanationGenerate = AuditLog.create(
                2L, "ADMIN", 7L, "ADMIN:7",
                "AI_EXPLANATION_GENERATE_REQUEST", "QT_PASSAGE", 35L,
                null, "{\"createdCount\":3}", OffsetDateTime.parse("2026-06-15T10:00:00+09:00"));
        entityManager.persist(assetApprove);
        entityManager.persist(explanationGenerate);
        entityManager.flush();
        entityManager.clear();

        var page = repository.findAll(
                new AuditQueryRepository.Filter(
                        null, null,
                        List.of("AI_ASSET_APPROVE", "AI_REGENERATE_REQUEST", "AI_EXPLANATION_GENERATE_REQUEST"),
                        null, null, null, null),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id")));

        assertThat(page.content())
                .extracting(AuditQueryRepository.AuditLogRow::actionType)
                .contains("AI_EXPLANATION_GENERATE_REQUEST", "AI_ASSET_APPROVE");
        assertThat(page.content())
                .filteredOn(row -> "AI_EXPLANATION_GENERATE_REQUEST".equals(row.actionType()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.targetType()).isEqualTo("QT_PASSAGE");
                    assertThat(row.targetId()).isEqualTo(35L);
                });
    }
}
