package com.qtai.domain.ai.internal;

import java.util.List;

record CommentaryMaterialContext(
        String commentarySource,
        String sourceName,
        String licenseLabel,
        String copyrightNotice,
        List<Long> commentaryMaterialIds,
        String verseRange,
        List<MaterialExcerpt> materials
) {

    static CommentaryMaterialContext empty() {
        return new CommentaryMaterialContext(null, null, null, null, List.of(), null, List.of());
    }

    boolean hasMaterials() {
        return !materials.isEmpty();
    }

    record MaterialExcerpt(
            Long materialId,
            String refs,
            String title,
            String excerpt,
            List<Long> verseIds
    ) {
    }
}
