package com.qtai.domain.note.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.note.api.CreateNoteUseCase;
import com.qtai.domain.note.api.DeleteNoteUseCase;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.JournalChangedEvent;
import com.qtai.domain.note.api.JournalEventType;
import com.qtai.domain.note.api.ListNoteCategoriesUseCase;
import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;
import com.qtai.domain.note.api.UpdateNoteUseCase;
import com.qtai.domain.note.api.dto.CreateNoteCommand;
import com.qtai.domain.note.api.dto.NoteCategoryItem;
import com.qtai.domain.note.api.dto.NoteCategoryResponse;
import com.qtai.domain.note.api.dto.NoteDetailResponse;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import com.qtai.domain.note.api.dto.NoteListItem;
import com.qtai.domain.note.api.dto.NoteListResponse;
import com.qtai.domain.note.api.dto.NoteCreateResponse;
import com.qtai.domain.note.api.dto.NoteUpdateResponse;
import com.qtai.domain.note.api.dto.NoteVerseItem;
import com.qtai.domain.note.api.dto.UpdateNoteCommand;
import com.qtai.domain.note.client.qt.NoteQtClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService implements ListNotesUseCase, GetNoteUseCase, CreateNoteUseCase,
        UpdateNoteUseCase, DeleteNoteUseCase, ListNoteCategoriesUseCase {

    private static final String DEFAULT_SORT = "updatedAt,desc";

    private final NoteRepository noteRepository;
    private final NoteVerseRepository noteVerseRepository;
    private final GetBibleVerseUseCase getBibleVerseUseCase;
    private final NoteQtClient noteQtClient;
    // 나눔 글 원본 삭제 통지(명세 §4.3.7) — sharing api 포트 (CLAUDE.md §4)
    private final com.qtai.domain.sharing.api.MarkSourceNoteDeletedUseCase markSourceNoteDeletedUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public NoteListResponse list(Long memberId, NoteCategory category, NoteStatus status, String q, Pageable pageable) {
        Page<Note> page = noteRepository.search(memberId, category, status, escapeLikeWildcards(q), pageable);
        List<NoteListItem> content = page.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new NoteListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                formatSort(pageable.getSort())
        );
    }

    @Override
    public NoteDetailResponse get(Long memberId, Long noteId) {
        Note note = noteRepository.findActiveByIdAndMemberId(noteId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        return toDetailResponse(note);
    }

    @Override
    public NoteDraftResponse getDraft(Long memberId, NoteCategory category, Long qtPassageId) {
        if (category != NoteCategory.MEDITATION || qtPassageId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return noteRepository.findDraft(memberId, category, qtPassageId)
                .map(note -> new NoteDraftResponse(true, toDetailResponse(note)))
                .orElseGet(() -> new NoteDraftResponse(false, null));
    }

    @Override
    @Transactional
    public NoteCreateResponse create(Long memberId, CreateNoteCommand command) {
        NormalizedNoteInput input = normalize(command);
        validateForSave(memberId, null, input);

        Note note = Note.create(
                memberId,
                input.qtPassageId(),
                input.category(),
                input.status(),
                input.visibility(),
                input.title(),
                input.body(),
                input.rememberSection(),
                input.interpretSection(),
                input.applySection(),
                input.praySection(),
                LocalDateTime.now(clock)
        );

        Note saved = saveNoteForCreate(note);
        replaceNoteVerses(saved.getId(), input.verseIds());
        if (saved.getCategory() == NoteCategory.MEDITATION) {
            publishJournalEvent(saved, JournalEventType.JOURNAL_CREATED, null);
        }
        return new NoteCreateResponse(
                saved.getId(),
                saved.getCategory(),
                saved.getStatus(),
                saved.getVisibility(),
                null,
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public NoteUpdateResponse update(Long memberId, Long noteId, UpdateNoteCommand command) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!memberId.equals(note.getMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (note.isDeleted()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        NoteSnapshot before = NoteSnapshot.from(note);

        NormalizedNoteInput input = normalize(command);
        validateForSave(memberId, noteId, input);
        note.update(
                input.category(),
                input.qtPassageId(),
                input.status(),
                input.visibility(),
                input.title(),
                input.body(),
                input.rememberSection(),
                input.interpretSection(),
                input.applySection(),
                input.praySection(),
                LocalDateTime.now(clock)
        );
        replaceNoteVerses(note.getId(), input.verseIds());
        if (shouldPublishJournalUpdate(before, note)) {
            publishJournalEvent(note, JournalEventType.JOURNAL_UPDATED, before.status());
        }
        return new NoteUpdateResponse(
                note.getId(),
                note.getCategory(),
                note.getStatus(),
                note.getVisibility(),
                note.getActiveUniqueKey(),
                note.getSavedAt(),
                note.getUpdatedAt(),
                false
        );
    }

    @Override
    @Transactional
    public void delete(Long memberId, Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!memberId.equals(note.getMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (note.isDeleted()) {
            return;
        }
        NoteStatus previousStatus = note.getStatus();
        boolean meditationNote = note.getCategory() == NoteCategory.MEDITATION;
        LocalDateTime deletedAt = LocalDateTime.now(clock);
        note.delete(deletedAt);
        // 이 노트로 발행된 나눔 글에 원본 삭제 시각 기록(명세 §4.3.7 — 유지+안내).
        // 같은 트랜잭션에서 처리해 노트 삭제와 기록의 정합을 보장한다.
        markSourceNoteDeletedUseCase.markSourceNoteDeleted(noteId, deletedAt);
        if (meditationNote) {
            publishJournalEvent(note, JournalEventType.JOURNAL_DELETED, previousStatus);
        }
    }

    @Override
    public NoteCategoryResponse listCategories() {
        return new NoteCategoryResponse(List.of(
                new NoteCategoryItem(NoteCategory.MEDITATION, "묵상 노트", true, true, false),
                new NoteCategoryItem(NoteCategory.SERMON, "설교 노트", false, true, true),
                new NoteCategoryItem(NoteCategory.PRAYER, "기도 노트", false, true, true),
                new NoteCategoryItem(NoteCategory.REPENTANCE, "회개 노트", false, true, true),
                new NoteCategoryItem(NoteCategory.GRATITUDE, "감사 노트", false, true, true)
        ));
    }

    private void validateForSave(Long memberId, Long currentNoteId, NormalizedNoteInput input) {
        if (input.category() == NoteCategory.MEDITATION) {
            if (input.qtPassageId() == null) {
                throw new BusinessException(ErrorCode.NOTE_QT_PASSAGE_REQUIRED);
            }
            noteQtClient.validateReadable(memberId, input.qtPassageId());
            validateMeditationDuplicate(memberId, currentNoteId, input.qtPassageId());
            return;
        }

        if (input.qtPassageId() != null) {
            throw new BusinessException(ErrorCode.NOTE_QT_PASSAGE_FORBIDDEN);
        }
        if (input.category() == NoteCategory.SERMON && input.verseIds().isEmpty()) {
            throw new BusinessException(ErrorCode.NOTE_VERSE_REQUIRED);
        }
    }

    private Note saveNoteForCreate(Note note) {
        try {
            return noteRepository.saveAndFlush(note);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_NOTE);
        }
    }

    private boolean shouldPublishJournalUpdate(NoteSnapshot before, Note note) {
        boolean relatedToMeditation = before.category() == NoteCategory.MEDITATION
                || note.getCategory() == NoteCategory.MEDITATION;
        if (!relatedToMeditation) {
            return false;
        }
        return before.category() != note.getCategory()
                || !Objects.equals(before.qtPassageId(), note.getQtPassageId())
                || before.status() != note.getStatus()
                || !Objects.equals(before.savedAt(), note.getSavedAt())
                || !Objects.equals(before.deletedAt(), note.getDeletedAt());
    }

    private void publishJournalEvent(Note note, JournalEventType eventType, NoteStatus previousStatus) {
        eventPublisher.publishEvent(new JournalChangedEvent(
                UUID.randomUUID(),
                note.getMemberId(),
                note.getId(),
                note.getQtPassageId(),
                eventType,
                previousStatus,
                note.getStatus(),
                LocalDateTime.now(clock)
        ));
    }

    private void validateMeditationDuplicate(Long memberId, Long currentNoteId, Long qtPassageId) {
        boolean exists = currentNoteId == null
                ? noteRepository.existsByMemberIdAndQtPassageIdAndCategoryAndActiveUniqueKey(
                memberId, qtPassageId, NoteCategory.MEDITATION, Note.ACTIVE_KEY)
                : noteRepository.existsByMemberIdAndQtPassageIdAndCategoryAndActiveUniqueKeyAndIdNot(
                memberId, qtPassageId, NoteCategory.MEDITATION, Note.ACTIVE_KEY, currentNoteId);
        if (exists) {
            throw new BusinessException(ErrorCode.DUPLICATE_NOTE);
        }
    }

    private void replaceNoteVerses(Long noteId, List<Long> verseIds) {
        noteVerseRepository.deleteByNoteId(noteId);
        getBibleVersesById(verseIds);

        List<NoteVerse> noteVerses = new ArrayList<>();
        short order = 1;
        for (Long verseId : verseIds) {
            noteVerses.add(NoteVerse.create(noteId, verseId, order++));
        }
        noteVerseRepository.saveAll(noteVerses);
    }

    private NoteDetailResponse toDetailResponse(Note note) {
        List<NoteVerse> noteVerses = noteVerseRepository.findAllByNoteIdOrderByDisplayOrderAsc(note.getId());
        Map<Long, BibleVerseResponse> versesById = getBibleVersesById(
                noteVerses.stream()
                        .map(NoteVerse::getBibleVerseId)
                        .toList()
        );
        List<NoteVerseItem> verseItems = noteVerses.stream()
                .map(noteVerse -> toVerseItem(noteVerse, versesById))
                .toList();
        return new NoteDetailResponse(
                note.getId(),
                note.getMemberId(),
                note.getCategory(),
                note.getQtPassageId(),
                note.getTitle(),
                note.getBody(),
                note.getRememberSection(),
                note.getInterpretSection(),
                note.getApplySection(),
                note.getPraySection(),
                note.getStatus(),
                note.getVisibility(),
                null,
                null,
                note.getVisibility() == NoteVisibility.SHARED,
                note.getSavedAt(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                verseItems
        );
    }

    private NoteListItem toListItem(Note note) {
        return new NoteListItem(
                note.getId(),
                note.getCategory(),
                note.getTitle(),
                note.getStatus(),
                note.getVisibility(),
                null,
                null,
                note.getVisibility() == NoteVisibility.SHARED,
                note.getSavedAt(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    private NoteVerseItem toVerseItem(NoteVerse noteVerse, Map<Long, BibleVerseResponse> versesById) {
        BibleVerseResponse verse = versesById.get(noteVerse.getBibleVerseId());
        return new NoteVerseItem(
                noteVerse.getBibleVerseId(),
                verse.bookCode(),
                verse.chapterNo(),
                verse.verseNo(),
                Integer.valueOf(noteVerse.getDisplayOrder())
        );
    }

    private Map<Long, BibleVerseResponse> getBibleVersesById(List<Long> verseIds) {
        if (verseIds.isEmpty()) {
            return Map.of();
        }
        List<BibleVerseResponse> verses = getBibleVerseUseCase.getVerses(verseIds);
        Map<Long, BibleVerseResponse> versesById = new LinkedHashMap<>();
        for (BibleVerseResponse verse : verses) {
            versesById.put(verse.id(), verse);
        }
        if (versesById.size() != verseIds.size()) {
            throw new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND);
        }
        return versesById;
    }

    private NormalizedNoteInput normalize(CreateNoteCommand command) {
        return normalize(
                command.category(),
                command.qtPassageId(),
                command.title(),
                command.body(),
                command.rememberSection(),
                command.interpretSection(),
                command.applySection(),
                command.praySection(),
                command.verseIds(),
                command.status(),
                command.visibility()
        );
    }

    private NormalizedNoteInput normalize(UpdateNoteCommand command) {
        return normalize(
                command.category(),
                command.qtPassageId(),
                command.title(),
                command.body(),
                command.rememberSection(),
                command.interpretSection(),
                command.applySection(),
                command.praySection(),
                command.verseIds(),
                command.status(),
                command.visibility()
        );
    }

    private NormalizedNoteInput normalize(NoteCategory category, Long qtPassageId, String title, String body,
                                          String rememberSection, String interpretSection, String applySection,
                                          String praySection, List<Long> verseIds, NoteStatus status,
                                          NoteVisibility visibility) {
        if (category == null || status == NoteStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        NoteStatus normalizedStatus = status == null ? NoteStatus.DRAFT : status;
        NoteVisibility normalizedVisibility = visibility == null ? NoteVisibility.PRIVATE : visibility;
        String normalizedTitle = trimToNull(title);
        String normalizedBody = trimToNull(body);
        String normalizedRememberSection = trimToNull(rememberSection);
        String normalizedInterpretSection = trimToNull(interpretSection);
        String normalizedApplySection = trimToNull(applySection);
        String normalizedPraySection = trimToNull(praySection);
        boolean hasSectionContent = normalizedRememberSection != null
                || normalizedInterpretSection != null
                || normalizedApplySection != null
                || normalizedPraySection != null;
        boolean hasRequiredContent = normalizedTitle != null
                || normalizedBody != null
                || (category == NoteCategory.MEDITATION && hasSectionContent);
        if (!hasRequiredContent) {
            throw new BusinessException(ErrorCode.NOTE_CONTENT_REQUIRED);
        }

        return new NormalizedNoteInput(
                category,
                qtPassageId,
                normalizedTitle == null ? "" : normalizedTitle,
                normalizedBody == null ? "" : normalizedBody,
                normalizedRememberSection,
                normalizedInterpretSection,
                normalizedApplySection,
                normalizedPraySection,
                normalizeVerseIds(verseIds),
                normalizedStatus,
                normalizedVisibility
        );
    }

    private List<Long> normalizeVerseIds(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> deduplicated = new LinkedHashMap<>();
        for (Long verseId : verseIds) {
            if (verseId == null || verseId < 1) {
                throw new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND);
            }
            deduplicated.putIfAbsent(verseId, verseId);
        }
        return List.copyOf(deduplicated.values());
    }

    private String formatSort(Sort sort) {
        return sort.stream()
                .findFirst()
                .map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase())
                .orElse(DEFAULT_SORT);
    }

    private static String escapeLikeWildcards(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record NormalizedNoteInput(
            NoteCategory category,
            Long qtPassageId,
            String title,
            String body,
            String rememberSection,
            String interpretSection,
            String applySection,
            String praySection,
            List<Long> verseIds,
            NoteStatus status,
            NoteVisibility visibility
    ) {
    }

    private record NoteSnapshot(
            NoteCategory category,
            Long qtPassageId,
            NoteStatus status,
            LocalDateTime savedAt,
            LocalDateTime deletedAt
    ) {

        static NoteSnapshot from(Note note) {
            return new NoteSnapshot(
                    note.getCategory(),
                    note.getQtPassageId(),
                    note.getStatus(),
                    note.getSavedAt(),
                    note.getDeletedAt()
            );
        }
    }
}
