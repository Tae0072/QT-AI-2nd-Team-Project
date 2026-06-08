package com.qtai.domain.study.internal;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.study.api.HidePublishedGlossaryTermsUseCase;
import com.qtai.domain.study.api.PublishApprovedGlossaryTermsUseCase;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsResult;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

        List<Long> bibleVerseIds = command.terms().stream()
                .map(PublishApprovedGlossaryTermsCommand.Term::bibleVerseId)
                .distinct()
                .toList();
        List<GlossaryTerm> existingVerseTerms =
                glossaryTermRepository.findApprovedByBibleVerseIdInForUpdate(bibleVerseIds);
        List<GlossaryTerm> existingTerms = mergeExistingTerms(existingAssetTerms, existingVerseTerms);
        if (existingTerms.size() == existingAssetTerms.size()
                && hasSameApprovedTerms(existingAssetTerms, command)) {
            return new PublishApprovedGlossaryTermsResult(command.aiAssetId(), 0, 0);
        }
        hideExistingTerms(existingTerms);

        LocalDateTime approvedAt = LocalDateTime.ofInstant(command.approvedAt().toInstant(), KST);
        List<GlossaryTerm> newTerms = command.terms().stream()
                .map(term -> GlossaryTerm.approvedFromAiAsset(
                        term.bibleVerseId(),
                        normalizeText(term.term()),
                        normalizeText(term.meaning()),
                        normalizeText(command.sourceLabel()),
                        command.aiAssetId(),
                        approvedAt
                ))
                .toList();
        List<GlossaryTerm> savedTerms = saveApprovedTerms(newTerms);

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
        if (command.terms().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "terms must not be empty; use hide command to unpublish glossary terms"
            );
        }
        for (PublishApprovedGlossaryTermsCommand.Term term : command.terms()) {
            requireValidTerm(term);
        }
        requireNoDuplicateVerses(command.terms());
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

    private static void requireNoDuplicateVerses(List<PublishApprovedGlossaryTermsCommand.Term> terms) {
        Set<Long> bibleVerseIds = new HashSet<>();
        for (PublishApprovedGlossaryTermsCommand.Term term : terms) {
            if (!bibleVerseIds.add(term.bibleVerseId())) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "terms must not contain duplicate bibleVerseId"
                );
            }
        }
    }

    private static List<GlossaryTerm> mergeExistingTerms(
            List<GlossaryTerm> existingAssetTerms,
            List<GlossaryTerm> existingVerseTerms
    ) {
        List<GlossaryTerm> mergedTerms = new ArrayList<>(existingAssetTerms);
        Set<Long> mergedTermIds = existingAssetTerms.stream()
                .map(GlossaryTerm::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        for (GlossaryTerm existingVerseTerm : existingVerseTerms) {
            Long existingVerseTermId = existingVerseTerm.getId();
            if (existingVerseTermId == null || mergedTermIds.add(existingVerseTermId)) {
                mergedTerms.add(existingVerseTerm);
            }
        }
        return mergedTerms;
    }

    private static boolean hasSameApprovedTerms(
            List<GlossaryTerm> existingAssetTerms,
            PublishApprovedGlossaryTermsCommand command
    ) {
        if (existingAssetTerms.size() != command.terms().size()) {
            return false;
        }
        Set<TermSnapshot> existingSnapshots = existingAssetTerms.stream()
                .map(TermSnapshot::from)
                .collect(Collectors.toSet());
        Set<TermSnapshot> requestedSnapshots = command.terms().stream()
                .map(term -> TermSnapshot.from(command, term))
                .collect(Collectors.toSet());
        return existingSnapshots.equals(requestedSnapshots);
    }

    private static String normalizeText(String value) {
        return value.trim();
    }

    private void hideExistingTerms(List<GlossaryTerm> terms) {
        terms.forEach(GlossaryTerm::hide);
        if (!terms.isEmpty()) {
            glossaryTermRepository.flush();
        }
    }

    private List<GlossaryTerm> saveApprovedTerms(List<GlossaryTerm> newTerms) {
        try {
            List<GlossaryTerm> savedTerms = glossaryTermRepository.saveAll(newTerms);
            glossaryTermRepository.flush();
            return savedTerms;
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "active glossary term already exists for bible verse"
            );
        }
    }

    private record TermSnapshot(Long bibleVerseId, String term, String meaning, String sourceLabel) {

        private static TermSnapshot from(GlossaryTerm glossaryTerm) {
            return new TermSnapshot(
                    glossaryTerm.getBibleVerseId(),
                    normalizeText(glossaryTerm.getTerm()),
                    normalizeText(glossaryTerm.getMeaning()),
                    normalizeText(glossaryTerm.getSourceLabel())
            );
        }

        private static TermSnapshot from(
                PublishApprovedGlossaryTermsCommand command,
                PublishApprovedGlossaryTermsCommand.Term term
        ) {
            return new TermSnapshot(
                    term.bibleVerseId(),
                    normalizeText(term.term()),
                    normalizeText(term.meaning()),
                    normalizeText(command.sourceLabel())
            );
        }
    }
}
