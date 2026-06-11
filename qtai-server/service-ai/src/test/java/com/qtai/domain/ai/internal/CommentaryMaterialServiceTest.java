package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class CommentaryMaterialServiceTest {

    private final CommentaryMaterialVerseRepository repository = mock(CommentaryMaterialVerseRepository.class);
    private final CommentaryMaterialService service = new CommentaryMaterialService(repository);

    @Test
    void findPromptContextByVerseIdsReturnsOneActiveMaterialWithSourceMetadata() {
        CommentarySource source = source(10L);
        CommentaryMaterial first = material(100L, source, "Gen.1.1", "first excerpt");
        CommentaryMaterial second = material(101L, source, "Gen.1.2", "second excerpt");
        when(repository.findActiveGenerationMappingsByVerseIds(List.of(1001L, 1002L))).thenReturn(List.of(
                mapping(first, 1001L, 1),
                mapping(first, 1002L, 2),
                mapping(second, 1002L, 3)
        ));

        CommentaryMaterialContext context = service.findPromptContextByVerseIds(List.of(1001L, 1002L));

        assertThat(context.hasMaterials()).isTrue();
        assertThat(context.commentarySource()).isEqualTo("TYNDALE_OPEN_STUDY_NOTES");
        assertThat(context.sourceName()).isEqualTo("Tyndale Open Study Notes");
        assertThat(context.licenseLabel()).isEqualTo("CC BY-SA 4.0");
        assertThat(context.copyrightNotice()).isEqualTo("Copyright notice");
        assertThat(context.commentaryMaterialIds()).containsExactly(100L);
        assertThat(context.materials()).hasSize(1);
        assertThat(context.materials().get(0).verseIds()).containsExactly(1001L, 1002L);
        assertThat(context.materials().get(0).excerpt()).isEqualTo("first excerpt");
    }

    @Test
    void findPromptContextByVerseIdsReturnsEmptyWhenNoMappings() {
        when(repository.findActiveGenerationMappingsByVerseIds(List.of(1001L))).thenReturn(List.of());

        CommentaryMaterialContext context = service.findPromptContextByVerseIds(List.of(1001L));

        assertThat(context.hasMaterials()).isFalse();
        assertThat(context.commentaryMaterialIds()).isEmpty();
    }

    private static CommentarySource source(Long id) {
        CommentarySource source = new CommentarySource();
        set(source, "id", id);
        set(source, "sourceKey", "TYNDALE_OPEN_STUDY_NOTES");
        set(source, "name", "Tyndale Open Study Notes");
        set(source, "sourceLabel", "Tyndale Open Study Notes");
        set(source, "licenseLabel", "CC BY-SA 4.0");
        set(source, "copyrightNotice", "Copyright notice");
        set(source, "status", CommentarySourceStatus.ACTIVE);
        set(source, "createdAt", OffsetDateTime.parse("2026-06-11T00:00:00Z"));
        return source;
    }

    private static CommentaryMaterial material(Long id, CommentarySource source, String refs, String contentText) {
        CommentaryMaterial material = new CommentaryMaterial();
        set(material, "id", id);
        set(material, "source", source);
        set(material, "externalId", "ext-" + id);
        set(material, "materialType", "study_note");
        set(material, "refs", refs);
        set(material, "bookCode", "Gen");
        set(material, "chapterStart", 1);
        set(material, "verseStart", 1);
        set(material, "chapterEnd", 1);
        set(material, "verseEnd", 2);
        set(material, "title", "Creation");
        set(material, "contentText", contentText);
        set(material, "contentHash", "hash-" + id);
        set(material, "status", CommentaryMaterialStatus.ACTIVE);
        set(material, "createdAt", OffsetDateTime.parse("2026-06-11T00:00:00Z"));
        return material;
    }

    private static CommentaryMaterialVerse mapping(CommentaryMaterial material, Long verseId, Integer displayOrder) {
        CommentaryMaterialVerse mapping = new CommentaryMaterialVerse();
        set(mapping, "material", material);
        set(mapping, "bibleVerseId", verseId);
        set(mapping, "displayOrder", displayOrder);
        set(mapping, "matchType", "RANGE_EXPANDED");
        return mapping;
    }

    private static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
