package com.qtai.domain.study.internal;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.study.api.HidePublishedGlossaryTermsUseCase;
import com.qtai.domain.study.api.PublishApprovedGlossaryTermsUseCase;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsResult;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlossaryTermService implements PublishApprovedGlossaryTermsUseCase, HidePublishedGlossaryTermsUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GlossaryTermRepository glossaryTermRepository;

    @Override
    @Transactional
    public PublishApprovedGlossaryTermsResult publishApprovedGlossaryTerms(
            PublishApprovedGlossaryTermsCommand command
    ) {
        requireValidCommand(command);
        List<GlossaryTerm> existingAssetTerms =
                glossaryTermRepository.findApprovedByAiAssetIdForUpdate(command.aiAssetId());
        if (command.terms().isEmpty()) {
            hideExistingTerms(existingAssetTerms);
            return new PublishApprovedGlossaryTermsResult(command.aiAssetId(), 0, existingAssetTerms.size());
        }

        List<Long> bibleVerseIds = command.terms().stream()
                .map(PublishApprovedGlossaryTermsCommand.Term::bibleVerseId)
                .distinct()
                .toList();
        List<GlossaryTerm> existingVerseTerms =
                glossaryTermRepository.findApprovedByBibleVerseIdInForUpdate(bibleVerseIds);
        List<GlossaryTerm> existingTerms = mergeExistingTerms(existingAssetTerms, existingVerseTerms);
        hideExistingTerms(existingTerms);

        LocalDateTime approvedAt = LocalDateTime.ofInstant(command.approvedAt().toInstant(), KST);
        List<GlossaryTerm> newTerms = command.terms().stream()
                .map(term -> GlossaryTerm.approvedFromAiAsset(
                        term.bibleVerseId(),
                        term.term(),
                        term.meaning(),
                        command.sourceLabel(),
                        command.aiAssetId(),
                        approvedAt
                ))
                .toList();
        List<GlossaryTerm> savedTerms = glossaryTermRepository.saveAll(newTerms);

        return new PublishApprovedGlossaryTermsResult(
                command.aiAssetId(),
                savedTerms.size(),
                existingTerms.size()
        );
    }

    @Override
    @Transactional
    public HidePublishedGlossaryTermsResult hidePublishedGlossaryTerms(
            HidePublishedGlossaryTermsCommand command
    ) {
        requireValidCommand(command);

        List<GlossaryTerm> terms = glossaryTermRepository.findApprovedByAiAssetIdForUpdate(command.aiAssetId());
        terms.forEach(GlossaryTerm::hide);
        if (!terms.isEmpty()) {
            glossaryTermRepository.flush();
        }

        return new HidePublishedGlossaryTermsResult(command.aiAssetId(), terms.size());
    }

    private static void requireValidCommand(PublishApprovedGlossaryTermsCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.aiAssetId(), "aiAssetId");
        requireText(command.sourceLabel(), "sourceLabel");
        if (command.approvedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "approvedAt must not be null");
        }
        if (command.terms() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "terms must not be null");
        }
        for (PublishApprovedGlossaryTermsCommand.Term term : command.terms()) {
            requireValidTerm(term);
        }
    }

    private static void requireValidTerm(PublishApprovedGlossaryTermsCommand.Term term) {
        if (term == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "term item must not be null");
        }
        requirePositive(term.bibleVerseId(), "bibleVerseId");
        requireText(term.term(), "term");
        requireText(term.meaning(), "meaning");
    }

    private static void requireValidCommand(HidePublishedGlossaryTermsCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.aiAssetId(), "aiAssetId");
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

    private static List<GlossaryTerm> mergeExistingTerms(
            List<GlossaryTerm> existingAssetTerms,
            List<GlossaryTerm> existingVerseTerms
    ) {
        List<GlossaryTerm> mergedTerms = new ArrayList<>(existingAssetTerms);
        for (GlossaryTerm existingVerseTerm : existingVerseTerms) {
            if (!mergedTerms.contains(existingVerseTerm)) {
                mergedTerms.add(existingVerseTerm);
            }
        }
        return mergedTerms;
    }

    private void hideExistingTerms(List<GlossaryTerm> terms) {
        terms.forEach(GlossaryTerm::hide);
        if (!terms.isEmpty()) {
            glossaryTermRepository.flush();
        }
    }
}
