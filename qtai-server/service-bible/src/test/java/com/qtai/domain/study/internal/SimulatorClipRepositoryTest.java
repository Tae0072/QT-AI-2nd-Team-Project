package com.qtai.domain.study.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.qtai.bible.BibleServiceApplication;
import com.qtai.bible.JpaAuditingConfig;
import com.qtai.common.config.TimeConfig;
import com.qtai.support.TestEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;

/**
 * {@link SimulatorClipRepository#findForAdmin} 슬라이스 테스트 (AD-14, F-06).
 * status/qtPassageId optional 조합이 실제 JPA 매핑에서 의도대로 필터링되는지 검증한다.
 */
@DataJpaTest
@ContextConfiguration(classes = BibleServiceApplication.class)
@Import({JpaAuditingConfig.class, TimeConfig.class})
class SimulatorClipRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SimulatorClipRepository repository;

    private SimulatorComponentLibraryVersion sharedVersion;

    @Test
    @DisplayName("findForAdmin: status/qtPassageId optional 조합으로 필터링한다")
    void findForAdmin_filtersByStatusAndPassage() throws Exception {
        // version 컬럼이 UNIQUE라 클립마다 새 버전을 쓰면 충돌 → 공유 버전 1개를 재사용한다.
        sharedVersion = TestEntityFactory.simulatorComponentLibraryVersion("2026.05.1");
        entityManager.persist(sharedVersion);

        persistClip(35L, SimulatorClipStatus.APPROVED);
        persistClip(35L, SimulatorClipStatus.PENDING);
        persistClip(99L, SimulatorClipStatus.APPROVED);
        entityManager.flush();
        entityManager.clear();

        var pageable = PageRequest.of(0, 20);

        // 둘 다 미지정 → 전체
        assertThat(repository.findForAdmin(null, null, pageable).getTotalElements()).isEqualTo(3);
        // status만
        assertThat(repository.findForAdmin(SimulatorClipStatus.APPROVED, null, pageable).getTotalElements())
                .isEqualTo(2);
        // qtPassageId만
        assertThat(repository.findForAdmin(null, 35L, pageable).getTotalElements()).isEqualTo(2);
        // 둘 다
        assertThat(repository.findForAdmin(SimulatorClipStatus.APPROVED, 35L, pageable).getTotalElements())
                .isEqualTo(1);
        // 결과 없음
        assertThat(repository.findForAdmin(SimulatorClipStatus.HIDDEN, null, pageable).getTotalElements())
                .isZero();
    }

    private void persistClip(Long qtPassageId, SimulatorClipStatus status) throws Exception {
        SimulatorClip clip = TestEntityFactory.simulatorClip(null, qtPassageId, status, "{}");
        // 팩토리는 클립마다 새 version을 만들어 UNIQUE(version) 충돌 → 공유 버전으로 교체.
        java.lang.reflect.Field field = SimulatorClip.class.getDeclaredField("componentLibraryVersion");
        field.setAccessible(true);
        field.set(clip, sharedVersion);
        entityManager.persist(clip);
    }
}
