package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.hibernate.exception.ConstraintViolationException;

@DataJpaTest
@ActiveProfiles("test")
class AiValidationChecklistVersionRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-05-27T10:00:00+09:00");

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AiValidationChecklistVersionRepository repository;

    @Test
    void checklistTypeAndVersionAreUnique() {
        persist(AiValidationChecklistType.EXPLANATION, "2026.05.1", BASE_TIME);
        flushAndClear();

        AiValidationChecklistVersion duplicate = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.05.1",
                "sha256:duplicate",
                null,
                BASE_TIME.plusMinutes(1)
        );

        assertThatThrownBy(() -> {
            testEntityManager.persist(duplicate);
            testEntityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void findAllByFiltersAppliesTypeStatusAndCreatedAtIdDescSort() {
        AiValidationChecklistVersion older = persist(
                AiValidationChecklistType.EXPLANATION,
                "2026.05.1",
                BASE_TIME.minusHours(1)
        );
        older.activate(BASE_TIME.minusMinutes(50));
        AiValidationChecklistVersion firstSameTime = persist(
                AiValidationChecklistType.EXPLANATION,
                "2026.05.2",
                BASE_TIME
        );
        firstSameTime.activate(BASE_TIME.plusMinutes(1));
        AiValidationChecklistVersion secondSameTime = persist(
                AiValidationChecklistType.EXPLANATION,
                "2026.05.3",
                BASE_TIME
        );
        secondSameTime.activate(BASE_TIME.plusMinutes(2));
        persist(AiValidationChecklistType.QA, "2026.05.1", BASE_TIME.plusMinutes(1));
        flushAndClear();

        Page<AiValidationChecklistVersion> page = repository.findAllByFilters(
                AiValidationChecklistType.EXPLANATION,
                AiValidationChecklistStatus.ACTIVE,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")))
        );

        assertThat(page.getTotalElements()).isEqualTo(3L);
        assertThat(page.getContent())
                .extracting(AiValidationChecklistVersion::getId)
                .containsExactly(secondSameTime.getId(), firstSameTime.getId(), older.getId());
    }

    @Test
    void findActiveVersionsByTypeReturnsOnlyActiveRows() {
        AiValidationChecklistVersion active = persist(AiValidationChecklistType.SIMULATOR, "2026.05.1", BASE_TIME);
        active.activate(BASE_TIME.plusMinutes(1));
        persist(AiValidationChecklistType.SIMULATOR, "2026.05.2", BASE_TIME.plusMinutes(2));
        persist(AiValidationChecklistType.EXPLANATION, "2026.05.1", BASE_TIME.plusMinutes(3));
        flushAndClear();

        assertThat(repository.findByChecklistTypeAndStatus(AiValidationChecklistType.SIMULATOR,
                AiValidationChecklistStatus.ACTIVE))
                .extracting(AiValidationChecklistVersion::getId)
                .containsExactly(active.getId());
    }

    private AiValidationChecklistVersion persist(
            AiValidationChecklistType type,
            String version,
            OffsetDateTime createdAt
    ) {
        return testEntityManager.persistAndFlush(AiValidationChecklistVersion.create(
                type,
                version,
                "sha256:" + type + "-" + version,
                null,
                createdAt
        ));
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }
}
