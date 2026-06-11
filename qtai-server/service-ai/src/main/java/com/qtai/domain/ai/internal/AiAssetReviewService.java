package com.qtai.domain.ai.internal;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.asset.ReviewAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetResult;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.study.api.HidePublishedGlossaryTermsUseCase;
import com.qtai.domain.study.api.HidePublishedVerseExplanationUseCase;
import com.qtai.domain.study.api.PublishApprovedGlossaryTermsUseCase;
import com.qtai.domain.study.api.PublishApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;

@Slf4j
@Service
class AiAssetReviewService implements ReviewAiAssetUseCase {

    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String TARGET_TYPE_AI_GENERATED_ASSET = "AI_GENERATED_ASSET";
    private static final String ACTION_AI_ASSET_APPROVE = "AI_ASSET_APPROVE";
    private static final String ACTION_AI_ASSET_REJECT = "AI_ASSET_REJECT";
    private static final String ACTION_AI_ASSET_HIDE = "AI_ASSET_HIDE";
    private static final int SERVER_AUTO_VALIDATION_LAYER = 1;
    private static final int AI_REVIEW_VALIDATION_LAYER = 2;

    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final AiValidationLogRepository validationLogRepository;
    private final PublishApprovedVerseExplanationUseCase publishApprovedVerseExplanationUseCase;
    private final HidePublishedVerseExplanationUseCase hidePublishedVerseExplanationUseCase;
    private final PublishApprovedGlossaryTermsUseCase publishApprovedGlossaryTermsUseCase;
    private final HidePublishedGlossaryTermsUseCase hidePublishedGlossaryTermsUseCase;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;

    AiAssetReviewService(
            AiGeneratedAssetRepository generatedAssetRepository,
            AiValidationLogRepository validationLogRepository,
            PublishApprovedVerseExplanationUseCase publishApprovedVerseExplanationUseCase,
            HidePublishedVerseExplanationUseCase hidePublishedVerseExplanationUseCase,
            PublishApprovedGlossaryTermsUseCase publishApprovedGlossaryTermsUseCase,
            HidePublishedGlossaryTermsUseCase hidePublishedGlossaryTermsUseCase,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper
    ) {
        this.generatedAssetRepository = generatedAssetRepository;
        this.validationLogRepository = validationLogRepository;
        this.publishApprovedVerseExplanationUseCase = publishApprovedVerseExplanationUseCase;
        this.hidePublishedVerseExplanationUseCase = hidePublishedVerseExplanationUseCase;
        this.publishApprovedGlossaryTermsUseCase = publishApprovedGlossaryTermsUseCase;
        this.hidePublishedGlossaryTermsUseCase = hidePublishedGlossaryTermsUseCase;
        this.auditLogUseCase = auditLogUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ReviewAiAssetResult reviewAiAsset(ReviewAiAssetCommand command) {
        requireValidCommand(command);
        requireAuthorizedReviewer(command.memberRole(), command.adminRole());

        AiAssetReviewAction action = parseAction(command.action());
        AiGeneratedAsset asset = generatedAssetRepository.findById(command.assetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND));
        String beforeJson = assetSnapshot(command.assetId(), asset);

        switch (action) {
            case APPROVE -> approve(command, asset);
            case REJECT -> asset.reject(command.reviewedAt());
            case HIDE -> hide(command, asset);
        }

        writeReviewAudit(command, action, beforeJson, asset);
        return new ReviewAiAssetResult(command.assetId(), asset.getStatus().name());
    }

    private void approve(ReviewAiAssetCommand command, AiGeneratedAsset asset) {
        requireValidatingAsset(asset);
        requireApprovableAssetType(asset);

        requirePassedValidationLog(
                asset.getId(),
                SERVER_AUTO_VALIDATION_LAYER,
                AiValidationReviewerType.AUTO,
                "AI asset has no server auto validation log",
                "AI asset latest server auto validation log must be PASSED"
        );
        requirePassedValidationLog(
                asset.getId(),
                AI_REVIEW_VALIDATION_LAYER,
                AiValidationReviewerType.ADVISOR,
                "AI asset has no advisor validation log",
                "AI asset latest advisor validation log must be PASSED"
        );

        PublishCommands publishCommands = publishCommandsForTarget(command, asset);
        asset.approve(command.reviewedAt());
        // study publish commands replace by aiAssetId/verse; a failed later call rolls back locally and retry replays both.
        if (publishCommands.verseExplanationCommand() != null) {
            publishVerseExplanation(publishCommands.verseExplanationCommand());
        }
        if (publishCommands.glossaryTermsCommand() != null) {
            publishGlossaryTerms(publishCommands.glossaryTermsCommand());
        }
    }

    private void publishVerseExplanation(PublishApprovedVerseExplanationCommand command) {
        try {
            publishApprovedVerseExplanationUseCase.publishApprovedVerseExplanation(command);
        } catch (RuntimeException exception) {
            log.warn("AI asset verse explanation publish failed. aiAssetId={}, bibleVerseId={}, errorType={}, errorMessage={}",
                    command.aiAssetId(), command.bibleVerseId(), exception.getClass().getSimpleName(),
                    exception.getMessage());
            throw exception;
        }
    }

    private void publishGlossaryTerms(PublishApprovedGlossaryTermsCommand command) {
        try {
            publishApprovedGlossaryTermsUseCase.publishApprovedGlossaryTerms(command);
        } catch (RuntimeException exception) {
            log.warn("AI asset glossary publish failed. aiAssetId={}, termCount={}, errorType={}, errorMessage={}",
                    command.aiAssetId(), command.terms().size(), exception.getClass().getSimpleName(),
                    exception.getMessage());
            throw exception;
        }
    }

    private PublishCommands publishCommandsForTarget(
            ReviewAiAssetCommand command,
            AiGeneratedAsset asset
    ) {
        if (!command.activateForTarget() || !isVerseExplanationBibleVerseAsset(asset)) {
            return new PublishCommands(null, null);
        }

        JsonNode root = payloadRoot(asset);
        return new PublishCommands(
                publishCommandForTarget(command, asset, root),
                glossaryCommand(command, asset, root)
        );
    }

    private PublishApprovedVerseExplanationCommand publishCommandForTarget(
            ReviewAiAssetCommand command,
            AiGeneratedAsset asset,
            JsonNode root
    ) {
        ExplanationItem explanationItem = explanationItemForTarget(asset, root);
        return new PublishApprovedVerseExplanationCommand(
                asset.getTargetId(),
                explanationItem.summary(),
                explanationItem.explanation(),
                requireText(asset.getSourceLabel(), "sourceLabel"),
                asset.getId(),
                command.reviewedAt()
        );
    }

    private void hide(ReviewAiAssetCommand command, AiGeneratedAsset asset) {
        asset.hide(command.reviewedAt());
        if (isVerseExplanationBibleVerseAsset(asset)) {
            hidePublishedVerseExplanationUseCase.hidePublishedVerseExplanation(
                    new HidePublishedVerseExplanationCommand(asset.getId())
            );
            hidePublishedGlossaryTermsUseCase.hidePublishedGlossaryTerms(
                    new HidePublishedGlossaryTermsCommand(asset.getId())
            );
        }
    }

    private static void requireValidatingAsset(AiGeneratedAsset asset) {
        if (asset.getStatus() != AiGeneratedAssetStatus.VALIDATING) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "AI asset must be VALIDATING to approve"
            );
        }
    }

    private static void requireApprovableAssetType(AiGeneratedAsset asset) {
        switch (asset.getAssetType()) {
            case EXPLANATION, SIMULATOR, QA_RESPONSE -> {
            }
            case SUMMARY, GLOSSARY -> throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "assetType does not support admin approval"
            );
        }
    }

    private void requirePassedValidationLog(
            Long assetId,
            int layer,
            AiValidationReviewerType reviewerType,
            String missingMessage,
            String failedMessage
    ) {
        AiValidationLog latestLog = validationLogRepository
                .findFirstByAiAssetIdAndLayerAndReviewerTypeOrderByCreatedAtDescIdDesc(
                        assetId,
                        layer,
                        reviewerType
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, missingMessage));
        if (latestLog.getResult() != AiValidationResult.PASSED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, failedMessage);
        }
    }

    private static boolean isVerseExplanationBibleVerseAsset(AiGeneratedAsset asset) {
        return asset.getAssetType() == AiGeneratedAssetType.EXPLANATION
                && asset.getTargetType() == AiTargetType.BIBLE_VERSE;
    }

    private ExplanationItem explanationItemForTarget(AiGeneratedAsset asset, JsonNode root) {
        JsonNode explanations = root.get("explanations");
        if (explanations == null || !explanations.isArray()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "asset payloadJson explanations is required");
        }
        for (JsonNode explanation : explanations) {
            JsonNode verseIdNode = explanation.get("verseId");
            if (verseIdNode != null
                    && verseIdNode.canConvertToLong()
                    && verseIdNode.asLong() == asset.getTargetId()) {
                return new ExplanationItem(
                        requireText(textValue(explanation.get("summary")), "summary"),
                        requireText(textValue(explanation.get("explanation")), "explanation")
                );
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "asset payloadJson target verse explanation is missing");
    }

    private PublishApprovedGlossaryTermsCommand glossaryCommand(
            ReviewAiAssetCommand command,
            AiGeneratedAsset asset,
            JsonNode root
    ) {
        JsonNode glossaryTerms = root.get("glossaryTerms");
        if (glossaryTerms == null || !glossaryTerms.isArray() || glossaryTerms.isEmpty()) {
            return null;
        }

        List<PublishApprovedGlossaryTermsCommand.Term> terms = glossaryTermItems(asset, glossaryTerms);
        if (terms.isEmpty()) {
            return null;
        }
        return new PublishApprovedGlossaryTermsCommand(
                asset.getId(),
                requireText(asset.getSourceLabel(), "sourceLabel"),
                command.reviewedAt(),
                terms
        );
    }

    private List<PublishApprovedGlossaryTermsCommand.Term> glossaryTermItems(
            AiGeneratedAsset asset,
            JsonNode glossaryTerms
    ) {
        List<PublishApprovedGlossaryTermsCommand.Term> terms = new ArrayList<>();
        for (JsonNode term : glossaryTerms) {
            JsonNode verseIdNode = term.get("verseId");
            if (verseIdNode == null || !verseIdNode.canConvertToLong()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "asset payloadJson glossaryTerms verseId is required");
            }
            long verseId = verseIdNode.asLong();
            if (verseId != asset.getTargetId()) {
                continue;
            }
            terms.add(new PublishApprovedGlossaryTermsCommand.Term(
                    verseId,
                    requireText(textValue(term.get("term")), "term"),
                    requireText(textValue(term.get("meaning")), "meaning")
            ));
        }
        return terms;
    }

    private JsonNode payloadRoot(AiGeneratedAsset asset) {
        try {
            JsonNode root = objectMapper.readTree(asset.getPayloadJson());
            if (root == null || !root.isObject()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "asset payloadJson must be a JSON object");
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "asset payloadJson is not valid JSON");
        }
    }

    private void writeReviewAudit(
            ReviewAiAssetCommand command,
            AiAssetReviewAction action,
            String beforeJson,
            AiGeneratedAsset asset
    ) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                null,
                ACTOR_TYPE_ADMIN,
                command.reviewerId(),
                ACTOR_TYPE_ADMIN + ":" + command.reviewerId(),
                action.auditActionType(),
                TARGET_TYPE_AI_GENERATED_ASSET,
                command.assetId(),
                beforeJson,
                reviewedAssetSnapshot(command.assetId(), asset, command.activateForTarget())
        ));
    }

    private String assetSnapshot(Long assetId, AiGeneratedAsset asset) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", assetId);
        payload.put("assetType", asset.getAssetType().name());
        payload.put("status", asset.getStatus().name());
        payload.put("targetType", asset.getTargetType().name());
        payload.put("targetId", asset.getTargetId());
        return toAuditJson(payload);
    }

    private String reviewedAssetSnapshot(Long assetId, AiGeneratedAsset asset, boolean activateForTarget) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", assetId);
        payload.put("assetType", asset.getAssetType().name());
        payload.put("status", asset.getStatus().name());
        payload.put("targetType", asset.getTargetType().name());
        payload.put("targetId", asset.getTargetId());
        payload.put("reviewedAt", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(asset.getReviewedAt()));
        payload.put("activateForTarget", activateForTarget);
        return toAuditJson(payload);
    }

    private String toAuditJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "audit snapshot serialization failed");
        }
    }

    private static AiAssetReviewAction parseAction(String value) {
        try {
            return AiAssetReviewAction.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "action is not supported");
        }
    }

    private static void requireValidCommand(ReviewAiAssetCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.reviewerId(), "reviewerId");
        requirePositive(command.assetId(), "assetId");
        requireText(command.memberRole(), "memberRole");
        requireText(command.adminRole(), "adminRole");
        requireText(command.action(), "action");
        requireText(command.reason(), "reason");
        if (command.reviewedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "reviewedAt must not be null");
        }
    }

    private static void requireAuthorizedReviewer(String memberRole, String adminRole) {
        if (!"ADMIN".equals(memberRole) || !("REVIEWER".equals(adminRole) || "SUPER_ADMIN".equals(adminRole))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
        return value;
    }

    private static String textValue(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return null;
        }
        return node.asText();
    }

    private enum AiAssetReviewAction {
        APPROVE(ACTION_AI_ASSET_APPROVE),
        REJECT(ACTION_AI_ASSET_REJECT),
        HIDE(ACTION_AI_ASSET_HIDE);

        private final String auditActionType;

        AiAssetReviewAction(String auditActionType) {
            this.auditActionType = auditActionType;
        }

        String auditActionType() {
            return auditActionType;
        }
    }

    private record ExplanationItem(
            String summary,
            String explanation
    ) {
    }

    private record PublishCommands(
            PublishApprovedVerseExplanationCommand verseExplanationCommand,
            PublishApprovedGlossaryTermsCommand glossaryTermsCommand
    ) {
    }
}
