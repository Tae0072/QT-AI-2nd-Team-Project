package com.qtai.domain.study.internal;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.study.api.HidePublishedSimulatorClipUseCase;
import com.qtai.domain.study.api.ListAdminSimulatorClipsUseCase;
import com.qtai.domain.study.api.PublishApprovedSimulatorClipUseCase;
import com.qtai.domain.study.api.dto.AdminSimulatorClipListItem;
import com.qtai.domain.study.api.dto.AdminSimulatorClipListResponse;
import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipCommand;
import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipResult;
import com.qtai.domain.study.api.dto.ListAdminSimulatorClipsQuery;
import com.qtai.domain.study.api.dto.PublishApprovedSimulatorClipCommand;
import com.qtai.domain.study.api.dto.PublishApprovedSimulatorClipResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시뮬레이터 클립 게시·숨김 (P1-11) — VerseExplanationService의 해설 publish/hide와 대칭.
 *
 * <p>게시 시점에 scene_script_json을 검증한다(빈 값·파싱 불가·과대 크기 차단) — 기존엔 검증이
 * 0이라 깨진 JSON이나 과대 페이로드가 그대로 노출 클립이 될 수 있었다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SimulatorClipPublishService implements
        PublishApprovedSimulatorClipUseCase, HidePublishedSimulatorClipUseCase,
        ListAdminSimulatorClipsUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** scene_script_json 최대 크기(문자) — LONGTEXT 범위 내 보호용 상한. */
    private static final int MAX_SCENE_SCRIPT_LENGTH = 200_000;
    private static final int MAX_PAGE_SIZE = 100;

    private final SimulatorClipRepository simulatorClipRepository;
    private final SimulatorComponentLibraryVersionRepository componentLibraryVersionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PublishApprovedSimulatorClipResult publishApprovedSimulatorClip(
            PublishApprovedSimulatorClipCommand command) {
        requireValidCommand(command);
        validateSceneScript(command.sceneScriptJson());

        SimulatorComponentLibraryVersion version = componentLibraryVersionRepository
                .findById(command.componentLibraryVersionId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "존재하지 않는 시뮬레이터 컴포넌트 라이브러리 버전: " + command.componentLibraryVersionId()));

        SimulatorClip saved = simulatorClipRepository.save(SimulatorClip.approvedFromAiAsset(
                command.qtPassageId(),
                command.title(),
                version,
                command.sceneScriptJson(),
                command.aiAssetId(),
                LocalDateTime.ofInstant(command.approvedAt().toInstant(), KST)
        ));

        return new PublishApprovedSimulatorClipResult(
                saved.getId(), saved.getQtPassageId(), saved.getStatus().name());
    }

    @Override
    @Transactional
    public HidePublishedSimulatorClipResult hidePublishedSimulatorClip(
            HidePublishedSimulatorClipCommand command) {
        if (command == null || command.aiAssetId() == null || command.aiAssetId() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "aiAssetId must be positive");
        }
        List<SimulatorClip> clips = simulatorClipRepository.findApprovedByAiAssetIdForUpdate(command.aiAssetId());
        clips.forEach(SimulatorClip::hide);
        if (!clips.isEmpty()) {
            simulatorClipRepository.flush();
        }
        return new HidePublishedSimulatorClipResult(command.aiAssetId(), clips.size());
    }

    @Override
    public AdminSimulatorClipListResponse listAdminSimulatorClips(ListAdminSimulatorClipsQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        if (query.page() < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page must not be negative");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size must be between 1 and 100");
        }
        SimulatorClipStatus status = parseStatus(query.status());
        Page<SimulatorClip> page = simulatorClipRepository.findForAdmin(
                status,
                query.qtPassageId(),
                PageRequest.of(query.page(), query.size(),
                        Sort.by(Sort.Direction.DESC, "approvedAt", "id")));

        return new AdminSimulatorClipListResponse(
                page.getContent().stream().map(SimulatorClipPublishService::toListItem).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private static AdminSimulatorClipListItem toListItem(SimulatorClip clip) {
        return new AdminSimulatorClipListItem(
                clip.getId(),
                clip.getQtPassageId(),
                clip.getTitle(),
                clip.getStatus().name(),
                clip.getAiAssetId(),
                clip.getApprovedAt() == null
                        ? null
                        : clip.getApprovedAt().atZone(KST).toOffsetDateTime()
        );
    }

    private static SimulatorClipStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return SimulatorClipStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status is not supported");
        }
    }

    private void validateSceneScript(String sceneScriptJson) {
        if (sceneScriptJson == null || sceneScriptJson.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "sceneScriptJson must not be blank");
        }
        if (sceneScriptJson.length() > MAX_SCENE_SCRIPT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "sceneScriptJson 크기가 허용 한도를 초과했습니다.");
        }
        try {
            objectMapper.readTree(sceneScriptJson);
        } catch (JsonProcessingException e) {
            // 광범위 catch(Exception) 금지(CLAUDE.md §9). readTree는 JSON 파싱 실패 시
            // JsonProcessingException만 던지므로 그 타입으로 좁힌다.
            throw new BusinessException(ErrorCode.INVALID_INPUT, "sceneScriptJson 형식이 올바르지 않습니다.");
        }
    }

    private static void requireValidCommand(PublishApprovedSimulatorClipCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.qtPassageId(), "qtPassageId");
        requireText(command.title(), "title");
        requirePositive(command.componentLibraryVersionId(), "componentLibraryVersionId");
        requirePositive(command.aiAssetId(), "aiAssetId");
        if (command.approvedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "approvedAt must not be null");
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
    }
}
