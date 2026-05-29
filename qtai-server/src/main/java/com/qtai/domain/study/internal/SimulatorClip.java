package com.qtai.domain.study.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "simulator_clips",
        indexes = {
                @Index(name = "idx_simulator_clips_passage_status", columnList = "qt_passage_id, status"),
                @Index(name = "idx_simulator_clips_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimulatorClip extends BaseEntity {

    @Column(name = "qt_passage_id", nullable = false)
    private Long qtPassageId;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_library_version_id", nullable = false)
    private SimulatorComponentLibraryVersion componentLibraryVersion;

    @Column(name = "scene_script_json", nullable = false, columnDefinition = "TEXT")
    private String sceneScriptJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SimulatorClipStatus status = SimulatorClipStatus.PENDING;

    @Column(name = "ai_asset_id")
    private Long aiAssetId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
