package com.qtai.domain.ai.internal;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Service
@Transactional(readOnly = true)
public class CommentaryMaterialService {

    private static final Logger log = LoggerFactory.getLogger(CommentaryMaterialService.class);

    private static final int MAX_EXCERPT_LENGTH = 1_200;
    private static final String SOURCE_MISMATCH_ERROR = "COMMENTARY_MATERIAL_SOURCE_MISMATCH";

    private final CommentaryMaterialVerseRepository repository;

    public CommentaryMaterialService(CommentaryMaterialVerseRepository repository) {
        this.repository = repository;
    }

    public CommentaryMaterialContext findPromptContextByVerseIds(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            return CommentaryMaterialContext.empty();
        }

        List<CommentaryMaterialVerse> mappings = repository.findActiveGenerationMappingsByVerseIds(verseIds);
        if (mappings.isEmpty()) {
            return CommentaryMaterialContext.empty();
        }

        Map<Long, MaterialAccumulator> selectedMaterials = new LinkedHashMap<>();
        for (CommentaryMaterialVerse mapping : mappings) {
            CommentaryMaterial material = mapping.getMaterial();
            selectedMaterials.computeIfAbsent(material.getId(), ignored -> new MaterialAccumulator(material))
                    .addVerseId(mapping.getBibleVerseId());
        }

        List<MaterialAccumulator> materialAccumulators = List.copyOf(selectedMaterials.values());
        CommentaryMaterial firstMaterial = materialAccumulators.get(0).material();
        CommentarySource source = firstMaterial.getSource();
        requireSingleSource(materialAccumulators, source);
        return new CommentaryMaterialContext(
                source.getSourceKey(),
                source.getName(),
                source.getLicenseLabel(),
                firstNonBlank(source.getCopyrightNotice(), source.getAttribution()),
                materialAccumulators.stream().map(accumulator -> accumulator.material().getId()).toList(),
                verseRange(firstMaterial),
                materialAccumulators.stream().map(MaterialAccumulator::toExcerpt).toList()
        );
    }

    private static void requireSingleSource(List<MaterialAccumulator> materialAccumulators, CommentarySource source) {
        String sourceKey = source == null ? null : source.getSourceKey();
        for (MaterialAccumulator accumulator : materialAccumulators) {
            CommentarySource candidateSource = accumulator.material().getSource();
            String candidateSourceKey = candidateSource == null ? null : candidateSource.getSourceKey();
            if (!Objects.equals(sourceKey, candidateSourceKey)) {
                log.warn(
                        "commentary material source mismatch: expectedSourceKey={}, actualSourceKey={}, materialId={}, verseIds={}",
                        sourceKey,
                        candidateSourceKey,
                        accumulator.material().getId(),
                        accumulator.verseIds()
                );
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        SOURCE_MISMATCH_ERROR
                );
            }
        }
    }

    private static String verseRange(CommentaryMaterial material) {
        return material.getBookCode()
                + "."
                + material.getChapterStart()
                + "."
                + material.getVerseStart()
                + "-"
                + material.getChapterEnd()
                + "."
                + material.getVerseEnd();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.strip();
        if (normalized.length() <= MAX_EXCERPT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_EXCERPT_LENGTH);
    }

    private static final class MaterialAccumulator {

        private final CommentaryMaterial material;
        private final LinkedHashSet<Long> verseIds = new LinkedHashSet<>();

        private MaterialAccumulator(CommentaryMaterial material) {
            this.material = material;
        }

        private CommentaryMaterial material() {
            return material;
        }

        private void addVerseId(Long verseId) {
            verseIds.add(verseId);
        }

        private List<Long> verseIds() {
            return List.copyOf(verseIds);
        }

        private CommentaryMaterialContext.MaterialExcerpt toExcerpt() {
            return new CommentaryMaterialContext.MaterialExcerpt(
                    material.getId(),
                    material.getRefs(),
                    material.getTitle(),
                    truncate(material.getContentText()),
                    List.copyOf(verseIds)
            );
        }
    }
}
