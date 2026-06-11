package com.qtai.domain.ai.internal;

import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentaryMaterialService {

    private static final int MAX_EXCERPT_LENGTH = 1_200;

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

        Long selectedMaterialId = null;
        MaterialAccumulator selected = null;
        // Prompt policy: use one active material by repository order, while collecting all mappings for that material.
        for (CommentaryMaterialVerse mapping : mappings) {
            CommentaryMaterial material = mapping.getMaterial();
            if (selectedMaterialId == null) {
                selectedMaterialId = material.getId();
                selected = new MaterialAccumulator(material);
            }
            if (!selectedMaterialId.equals(material.getId())) {
                continue;
            }
            selected.addVerseId(mapping.getBibleVerseId());
        }

        List<MaterialAccumulator> selectedMaterials = List.of(selected);
        CommentaryMaterial firstMaterial = selected.material();
        CommentarySource source = firstMaterial.getSource();
        return new CommentaryMaterialContext(
                source.getSourceKey(),
                source.getName(),
                source.getLicenseLabel(),
                firstNonBlank(source.getCopyrightNotice(), source.getAttribution()),
                selectedMaterials.stream().map(accumulator -> accumulator.material().getId()).toList(),
                verseRange(firstMaterial),
                selectedMaterials.stream().map(MaterialAccumulator::toExcerpt).toList()
        );
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
