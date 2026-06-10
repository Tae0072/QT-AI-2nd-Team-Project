package com.qtai.domain.study.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "simulator_component_library_versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimulatorComponentLibraryVersion extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String version;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}
