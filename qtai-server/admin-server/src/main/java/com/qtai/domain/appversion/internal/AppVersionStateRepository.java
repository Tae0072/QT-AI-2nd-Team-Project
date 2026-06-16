package com.qtai.domain.appversion.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 앱 버전 상태 영속성 포트. 단일 행만 유지한다(가장 낮은 id 1건).
 */
public interface AppVersionStateRepository extends JpaRepository<AppVersionState, Long> {

    Optional<AppVersionState> findTopByOrderByIdAsc();
}
