package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse.Day;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse.Summary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeditationCalendarService implements GetMeditationCalendarUseCase {

    private final NoteRepository noteRepository;
    private final Clock clock;

    @Override
    public MeditationCalendarResponse getCalendar(Long memberId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endExclusiveDate = month.plusMonths(1).atDay(1);
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endExclusiveDate.atStartOfDay();
        List<Note> notes = noteRepository.findSavedCalendarNotes(memberId, startAt, endAt);
        Map<LocalDate, List<Note>> notesByDate = notes.stream()
                .collect(Collectors.groupingBy(note -> note.getSavedAt().toLocalDate()));

        List<Day> days = new ArrayList<>();
        long savedNoteCount = 0;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            List<Note> dayNotes = notesByDate.getOrDefault(date, List.of());
            savedNoteCount += dayNotes.size();
            days.add(toDay(date, dayNotes));
        }

        long savedDays = days.stream()
                .filter(Day::saved)
                .count();
        long streakDays = calculateStreak(month, notesByDate.keySet());
        return new MeditationCalendarResponse(
                month.toString(),
                days,
                new Summary(savedDays, savedNoteCount, streakDays)
        );
    }

    private Day toDay(LocalDate date, List<Note> notes) {
        Long meditationNoteId = notes.stream()
                .filter(note -> note.getCategory() == NoteCategory.MEDITATION)
                .map(Note::getId)
                .findFirst()
                .orElse(null);
        List<NoteCategory> categories = notes.stream()
                .map(Note::getCategory)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
        return new Day(date, !notes.isEmpty(), notes.size(), meditationNoteId, categories);
    }

    private long calculateStreak(YearMonth month, Set<LocalDate> savedDates) {
        LocalDate today = LocalDate.now(clock);
        LocalDate monthStart = month.atDay(1);
        LocalDate anchor = min(month.atEndOfMonth(), today);
        if (anchor.isBefore(monthStart)) {
            return 0;
        }

        long streak = 0;
        LocalDate cursor = anchor;
        while (!cursor.isBefore(monthStart) && savedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static LocalDate min(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }
}
