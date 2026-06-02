package com.qtai.domain.study.internal;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.PublishApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerseExplanationService
        implements ListApprovedVerseExplanationUseCase, PublishApprovedVerseExplanationUseCase {

    private static final String ACTIVE_UNIQUE_KEY = "ACTIVE";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final VerseExplanationRepository verseExplanationRepository;

    @Override
    public List<ApprovedVerseExplanationResponse> listApprovedByVerseIds(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            return List.of();
        }

        return verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                        verseIds,
                        VerseExplanationStatus.APPROVED,
                        ACTIVE_UNIQUE_KEY
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PublishApprovedVerseExplanationResult publishApprovedVerseExplanation(
            PublishApprovedVerseExplanationCommand command
    ) {
        requireValidCommand(command);

        verseExplanationRepository.findActiveApprovedByBibleVerseIdForUpdate(command.bibleVerseId())
                .forEach(VerseExplanation::deactivate);
        verseExplanationRepository.flush();

        VerseExplanation saved = verseExplanationRepository.save(VerseExplanation.approvedFromAiAsset(
                command.bibleVerseId(),
                command.summary(),
                command.explanation(),
                command.sourceLabel(),
                command.aiAssetId(),
                LocalDateTime.ofInstant(command.approvedAt().toInstant(), KST)
        ));

        return new PublishApprovedVerseExplanationResult(
                saved.getBibleVerseId(),
                saved.getAiAssetId(),
                saved.getStatus().name()
        );
    }

    private ApprovedVerseExplanationResponse toResponse(VerseExplanation explanation) {
        return new ApprovedVerseExplanationResponse(
                explanation.getBibleVerseId(),
                explanation.getSummary(),
                explanation.getExplanation(),
                explanation.getSourceLabel(),
                explanation.getAiAssetId()
        );
    }

    private static void requireValidCommand(PublishApprovedVerseExplanationCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.bibleVerseId(), "bibleVerseId");
        requireText(command.summary(), "summary");
        requireText(command.explanation(), "explanation");
        requireText(command.sourceLabel(), "sourceLabel");
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
