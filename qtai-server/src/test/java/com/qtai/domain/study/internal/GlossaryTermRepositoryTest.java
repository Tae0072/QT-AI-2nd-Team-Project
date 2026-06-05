package com.qtai.domain.study.internal;

import com.qtai.config.JpaAuditingConfig;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class GlossaryTermRepositoryTest {

    @Autowired
    private GlossaryTermRepository glossaryTermRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findApprovedByAiAssetIdForUpdate returns only APPROVED terms")
    void findApprovedByAiAssetIdForUpdate_returnsOnlyApprovedTerms() {
        persistGlossaryTerm(10L, "approved", GlossaryTermStatus.APPROVED, 500L);
        persistGlossaryTerm(11L, "hidden", GlossaryTermStatus.HIDDEN, 500L);
        persistGlossaryTerm(12L, "other asset", GlossaryTermStatus.APPROVED, 501L);
        entityManager.flush();
        entityManager.clear();

        List<GlossaryTerm> result = glossaryTermRepository.findApprovedByAiAssetIdForUpdate(500L);

        assertThat(result)
                .extracting(GlossaryTerm::getAiAssetId, GlossaryTerm::getBibleVerseId, GlossaryTerm::getTerm,
                        GlossaryTerm::getStatus)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        500L,
                        10L,
                        "approved",
                        GlossaryTermStatus.APPROVED
                ));
    }

    @Test
    @DisplayName("findApprovedByBibleVerseIdInForUpdate returns only APPROVED terms for requested verses")
    void findApprovedByBibleVerseIdInForUpdate_returnsOnlyApprovedTermsForRequestedVerses() {
        persistGlossaryTerm(10L, "approved 10", GlossaryTermStatus.APPROVED, 500L);
        persistGlossaryTerm(10L, "hidden 10", GlossaryTermStatus.HIDDEN, 500L);
        persistGlossaryTerm(11L, "approved 11", GlossaryTermStatus.APPROVED, 501L);
        persistGlossaryTerm(12L, "outside verse", GlossaryTermStatus.APPROVED, 502L);
        entityManager.flush();
        entityManager.clear();

        List<GlossaryTerm> result = glossaryTermRepository.findApprovedByBibleVerseIdInForUpdate(List.of(10L, 11L));

        assertThat(result)
                .extracting(GlossaryTerm::getBibleVerseId, GlossaryTerm::getTerm, GlossaryTerm::getStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(10L, "approved 10", GlossaryTermStatus.APPROVED),
                        org.assertj.core.groups.Tuple.tuple(11L, "approved 11", GlossaryTermStatus.APPROVED)
                );
    }

    @Test
    @DisplayName("repository locking queries keep PESSIMISTIC_WRITE lock mode")
    void lockingQueries_keepPessimisticWriteLockMode() throws NoSuchMethodException {
        assertPessimisticWriteLock("findApprovedByAiAssetIdForUpdate", Long.class);
        assertPessimisticWriteLock("findApprovedByBibleVerseIdInForUpdate", List.class);
    }

    private GlossaryTerm persistGlossaryTerm(
            Long bibleVerseId,
            String term,
            GlossaryTermStatus status,
            Long aiAssetId
    ) {
        GlossaryTerm glossaryTerm = GlossaryTerm.approvedFromAiAsset(
                bibleVerseId,
                term,
                "meaning",
                "QT-AI DeepSeek",
                aiAssetId,
                LocalDateTime.of(2026, 6, 4, 14, 20)
        );
        if (status == GlossaryTermStatus.HIDDEN) {
            glossaryTerm.hide();
        }
        entityManager.persist(glossaryTerm);
        return glossaryTerm;
    }

    private static void assertPessimisticWriteLock(String methodName, Class<?> parameterType)
            throws NoSuchMethodException {
        Method method = GlossaryTermRepository.class.getMethod(methodName, parameterType);
        Lock lock = method.getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
