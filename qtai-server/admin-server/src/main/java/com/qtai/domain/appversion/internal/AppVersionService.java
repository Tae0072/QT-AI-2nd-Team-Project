package com.qtai.domain.appversion.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.appversion.api.AdminAppVersionUseCase;
import com.qtai.domain.appversion.api.dto.AppVersionStateResponse;
import com.qtai.domain.appversion.api.dto.PendingAppUpdateCreateRequest;
import com.qtai.domain.appversion.api.dto.PendingAppUpdateResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 앱 버전/업데이트 관리 서비스 (appversion 도메인, 2026-06-14 Lead 승인).
 *
 * <p>admin-server 고유 기능. 콘텐츠 버전 즉시 게시와 앱 출시 버전 업데이트(업데이트 예정 큐)를 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppVersionService implements AdminAppVersionUseCase {

    private final AppVersionStateRepository stateRepository;
    private final PendingAppUpdateRepository pendingRepository;
    private final Clock clock;

    @Override
    @Transactional
    public AppVersionStateResponse getState() {
        return toResponse(getOrCreateState());
    }

    @Override
    @Transactional
    public AppVersionStateResponse applyContent() {
        AppVersionState state = getOrCreateState();
        state.bumpContentVersion();
        log.info("콘텐츠 버전 게시. contentVersion={}", state.getContentVersion());
        return toResponse(state);
    }

    @Override
    public List<PendingAppUpdateResponse> listPending(String status) {
        List<PendingAppUpdate> rows;
        if (status == null || status.isBlank()) {
            rows = pendingRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
        } else {
            rows = pendingRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                    parseStatus(status));
        }
        return rows.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PendingAppUpdateResponse createPending(PendingAppUpdateCreateRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "제목을 입력하세요.");
        }
        if (request.targetAppVersion() == null || request.targetAppVersion().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "대상 앱 버전을 입력하세요.");
        }
        PendingAppUpdate entity = PendingAppUpdate.builder()
                .title(request.title().strip())
                .description(request.description())
                .targetAppVersion(request.targetAppVersion().strip())
                .updateMode(parseMode(request.updateMode(), AppUpdateMode.RECOMMENDED))
                .build();
        PendingAppUpdate saved = pendingRepository.save(entity);
        log.info("업데이트 예정 등록. id={}, targetAppVersion={}", saved.getId(), saved.getTargetAppVersion());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AppVersionStateResponse applyPending(Long id) {
        PendingAppUpdate pending = pendingRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "업데이트 예정 항목을 찾을 수 없습니다: " + id));
        if (pending.getStatus() == PendingUpdateStatus.APPLIED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 적용된 항목입니다.");
        }
        AppVersionState state = getOrCreateState();
        state.promoteAppVersion(pending.getTargetAppVersion(), pending.getUpdateMode(),
                pending.getTitle());
        pending.markApplied(LocalDateTime.now(clock));
        log.info("앱 출시 버전 업데이트. appVersion={}, mode={}, pendingId={}",
                state.getAppVersion(), pending.getUpdateMode(), id);
        return toResponse(state);
    }

    @Override
    @Transactional
    public void deletePending(Long id) {
        PendingAppUpdate pending = pendingRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "업데이트 예정 항목을 찾을 수 없습니다: " + id));
        pending.softDelete(LocalDateTime.now(clock));
        log.info("업데이트 예정 삭제. id={}", id);
    }

    // ── helpers ──

    private AppVersionState getOrCreateState() {
        return stateRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> stateRepository.save(AppVersionState.builder().build()));
    }

    private static AppUpdateMode parseMode(String raw, AppUpdateMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return AppUpdateMode.valueOf(raw.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "updateMode는 NONE, RECOMMENDED, FORCED만 허용됩니다.");
        }
    }

    private static PendingUpdateStatus parseStatus(String raw) {
        try {
            return PendingUpdateStatus.valueOf(raw.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "status는 PENDING 또는 APPLIED만 허용됩니다.");
        }
    }

    private AppVersionStateResponse toResponse(AppVersionState s) {
        return new AppVersionStateResponse(
                s.getContentVersion(), s.getAppVersion(), s.getMinSupportedVersion(),
                s.getUpdateMode().name(), s.getUpdateMessage(), s.getUpdatedAt());
    }

    private PendingAppUpdateResponse toResponse(PendingAppUpdate p) {
        return new PendingAppUpdateResponse(
                p.getId(), p.getTitle(), p.getDescription(), p.getTargetAppVersion(),
                p.getUpdateMode().name(), p.getStatus().name(), p.getCreatedAt(), p.getAppliedAt());
    }
}
