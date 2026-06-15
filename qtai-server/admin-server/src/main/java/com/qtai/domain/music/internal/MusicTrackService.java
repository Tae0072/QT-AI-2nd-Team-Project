package com.qtai.domain.music.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.music.api.CreateAdminMusicTrackUseCase;
import com.qtai.domain.music.api.DeleteAdminMusicTrackUseCase;
import com.qtai.domain.music.api.GetMusicTrackAudioUseCase;
import com.qtai.domain.music.api.HideAdminMusicTrackUseCase;
import com.qtai.domain.music.api.ListAdminMusicTrackUseCase;
import com.qtai.domain.music.api.ListMusicTrackUseCase;
import com.qtai.domain.music.api.PublishAdminMusicTrackUseCase;
import com.qtai.domain.music.api.UpdateAdminMusicTrackUseCase;
import com.qtai.domain.music.api.dto.AdminMusicTrackCommand;
import com.qtai.domain.music.api.dto.AdminMusicTrackListResponse;
import com.qtai.domain.music.api.dto.AdminMusicTrackResponse;
import com.qtai.domain.music.api.dto.MusicTrackAudioResponse;
import com.qtai.domain.music.api.dto.MusicTrackResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배경음악 도메인 서비스.
 *
 * <p>사용자 앱에는 활성 음원 조회/스트리밍만 제공하고, 관리자 웹에는 등록·수정·노출·숨김을 제공한다.
 * 목록 조회는 projection으로 음원 바이트를 읽지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MusicTrackService implements
        ListMusicTrackUseCase,
        GetMusicTrackAudioUseCase,
        ListAdminMusicTrackUseCase,
        CreateAdminMusicTrackUseCase,
        UpdateAdminMusicTrackUseCase,
        PublishAdminMusicTrackUseCase,
        HideAdminMusicTrackUseCase,
        DeleteAdminMusicTrackUseCase {

    private final MusicTrackRepository musicTrackRepository;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final MusicTrackAuditSnapshotFactory auditSnapshotFactory;

    @Override
    public List<MusicTrackResponse> listEnabled() {
        return musicTrackRepository
                .findByEnabledTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MusicTrackAudioResponse getAudio(Long trackId) {
        MusicTrackAudioView audio = musicTrackRepository.findEnabledAudioById(trackId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "음원을 찾을 수 없습니다: " + trackId));
        return new MusicTrackAudioResponse(
                audio.getAudioData(), audio.getMimeType(), audio.getByteSize());
    }

    @Override
    public AdminMusicTrackListResponse listAdmin(String status, String category, Pageable pageable) {
        Boolean enabled = MusicTrackStatus.enabledFilter(status);
        MusicCategory categoryFilter = parseCategoryOrNull(category);
        Page<AdminMusicTrackResponse> page =
                musicTrackRepository.findAdminSummaries(enabled, categoryFilter, pageable)
                        .map(this::toAdminResponse);
        return new AdminMusicTrackListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                sort(pageable)
        );
    }

    @Override
    @Transactional
    public AdminMusicTrackResponse createAdmin(Long adminUserId, AdminMusicTrackCommand command) {
        validateRequiredAudio(command.audioData());
        MusicTrack track = MusicTrack.builder()
                .title(requiredText(command.title(), "title"))
                .category(parseCategory(command.category()))
                .mimeType(requiredText(command.mimeType(), "mimeType"))
                .byteSize((long) command.audioData().length)
                .durationSec(command.durationSec())
                .sortOrder(command.sortOrder())
                .enabled(false)
                .licenseNote(command.licenseNote())
                .audioData(command.audioData())
                .build();
        musicTrackRepository.save(track);
        writeAudit(adminUserId, "MUSIC_TRACK_CREATE", track.getId(), null,
                auditSnapshotFactory.snapshot(track));
        return toAdminResponse(track);
    }

    @Override
    @Transactional
    public AdminMusicTrackResponse updateAdmin(Long adminUserId, Long trackId,
                                               AdminMusicTrackCommand command) {
        MusicTrack track = findTrack(trackId);
        String before = auditSnapshotFactory.snapshot(track);
        track.updateMetadata(
                optionalText(command.title(), "title"),
                parseCategoryOrNull(command.category()),
                optionalText(command.mimeType(), "mimeType"),
                command.durationSec(),
                command.sortOrder(),
                command.licenseNote()
        );
        if (command.audioData() != null) {
            validateRequiredAudio(command.audioData());
            track.replaceAudio(command.mimeType(), command.audioData());
        }
        writeAudit(adminUserId, "MUSIC_TRACK_UPDATE", track.getId(), before,
                auditSnapshotFactory.snapshot(track));
        return toAdminResponse(track);
    }

    @Override
    @Transactional
    public AdminMusicTrackResponse publishAdmin(Long adminUserId, Long trackId) {
        MusicTrack track = findTrack(trackId);
        if (Boolean.TRUE.equals(track.getEnabled())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "이미 노출 중인 배경음악입니다.");
        }
        String before = auditSnapshotFactory.snapshot(track);
        track.publish();
        writeAudit(adminUserId, "MUSIC_TRACK_PUBLISH", track.getId(), before,
                auditSnapshotFactory.snapshot(track));
        return toAdminResponse(track);
    }

    @Override
    @Transactional
    public AdminMusicTrackResponse hideAdmin(Long adminUserId, Long trackId) {
        MusicTrack track = findTrack(trackId);
        if (!Boolean.TRUE.equals(track.getEnabled())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "이미 숨김 상태인 배경음악입니다.");
        }
        String before = auditSnapshotFactory.snapshot(track);
        track.disable();
        writeAudit(adminUserId, "MUSIC_TRACK_HIDE", track.getId(), before,
                auditSnapshotFactory.snapshot(track));
        return toAdminResponse(track);
    }

    @Override
    @Transactional
    public void deleteAdmin(Long adminUserId, Long trackId) {
        MusicTrack track = findTrack(trackId);
        String before = auditSnapshotFactory.snapshot(track);
        track.softDelete();
        writeAudit(adminUserId, "MUSIC_TRACK_DELETE", track.getId(), before,
                auditSnapshotFactory.snapshot(track));
    }

    private MusicTrack findTrack(Long trackId) {
        return musicTrackRepository.findById(trackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "배경음악을 찾을 수 없습니다: " + trackId));
    }

    private MusicTrackResponse toResponse(MusicTrackSummary s) {
        return new MusicTrackResponse(
                s.getId(),
                s.getTitle(),
                s.getCategory().name(),
                s.getMimeType(),
                s.getByteSize(),
                s.getDurationSec(),
                s.getSortOrder(),
                streamUrl(s.getId())
        );
    }

    private AdminMusicTrackResponse toAdminResponse(AdminMusicTrackSummary s) {
        return new AdminMusicTrackResponse(
                s.getId(),
                s.getTitle(),
                s.getCategory().name(),
                s.getMimeType(),
                s.getByteSize(),
                s.getDurationSec(),
                s.getSortOrder(),
                s.getLicenseNote(),
                MusicTrackStatus.fromEnabled(s.getEnabled()).name(),
                streamUrl(s.getId()),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    private AdminMusicTrackResponse toAdminResponse(MusicTrack track) {
        return new AdminMusicTrackResponse(
                track.getId(),
                track.getTitle(),
                track.getCategory().name(),
                track.getMimeType(),
                track.getByteSize(),
                track.getDurationSec(),
                track.getSortOrder(),
                track.getLicenseNote(),
                MusicTrackStatus.fromEnabled(track.getEnabled()).name(),
                streamUrl(track.getId()),
                track.getCreatedAt(),
                track.getUpdatedAt()
        );
    }

    private void writeAudit(Long adminUserId, String actionType, Long trackId,
                            String beforeJson, String afterJson) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                adminUserId,
                "ADMIN",
                adminUserId,
                "ADMIN:" + adminUserId,
                actionType,
                "MUSIC_TRACK",
                trackId,
                beforeJson,
                afterJson
        ));
    }

    private static String streamUrl(Long id) {
        return "/api/v1/music/tracks/" + id + "/stream";
    }

    private static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " 값은 필수입니다.");
        }
        return value.trim();
    }

    private static MusicCategory parseCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return MusicCategory.BGM;
        }
        try {
            return MusicCategory.valueOf(rawCategory);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "유효하지 않은 배경음악 분류입니다: " + rawCategory);
        }
    }

    private static MusicCategory parseCategoryOrNull(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return null;
        }
        return parseCategory(rawCategory);
    }

    private static String optionalText(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        return requiredText(value, fieldName);
    }

    private static void validateRequiredAudio(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "음원 파일은 필수입니다.");
        }
    }

    private static String sort(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return "";
        }
        return pageable.getSort().stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }
}
