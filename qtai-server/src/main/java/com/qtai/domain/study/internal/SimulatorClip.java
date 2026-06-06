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

    /**
     * AI 산출물 승인본으로부터 사용자 노출용 시뮬레이터 클립을 생성한다 (P1-11).
     *
     * <p>VerseExplanation.approvedFromAiAsset과 대칭. 같은 본문에 여러 클립이 존재할 수 있고
     * 사용자 조회는 최신 APPROVED를 고르므로 active_unique_key는 두지 않는다.
     */
    public static SimulatorClip approvedFromAiAsset(
            Long qtPassageId,
            String title,
            SimulatorComponentLibraryVersion componentLibraryVersion,
            String sceneScriptJson,
            Long aiAssetId,
            LocalDateTime approvedAt) {
        SimulatorClip clip = new SimulatorClip();
        clip.qtPassageId = qtPassageId;
        clip.title = title;
        clip.componentLibraryVersion = componentLibraryVersion;
        clip.sceneScriptJson = sceneScriptJson;
        clip.aiAssetId = aiAssetId;
        clip.status = SimulatorClipStatus.APPROVED;
        clip.approvedAt = approvedAt;
        return clip;
    }

    /** 승인 노출본을 숨긴다(긴급 차단·재검토). APPROVED → HIDDEN. */
    public void hide() {
        this.status = SimulatorClipStatus.HIDDEN;
    }
}
