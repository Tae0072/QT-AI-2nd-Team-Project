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
        long streakDays = calculateStreak(memberId);
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

    /** streak 윈도(일) — 이 기간을 넘는 연속 묵상은 집계하지 않는다(실무상 충분). */
    private static final int STREAK_WINDOW_DAYS = 400;

    /**
     * 연속 묵상 일수(streak)를 계산한다 (P1-9, 2026-06-05 Lead 결정).
     *
     * <p>버그 수정: 기존엔 오늘이 미저장이면 anchor(오늘)에서 즉시 끊겨 04:00 배치 시점에는
     * streak이 사실상 항상 0이었고, 월 경계에서 잘려 연속이 끊겼다.
     * <ul>
     *   <li>anchor = 오늘 저장돼 있으면 오늘, 아니면 어제(어제까지 연속 인정)</li>
     *   <li>월 경계를 넘어 연속으로 카운트(달력 월 범위가 아니라 윈도 조회)</li>
     * </ul>
     */
    private long calculateStreak(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate windowStart = today.minusDays(STREAK_WINDOW_DAYS);
        // anchor 다음날 00:00까지 포함하도록 endExclusive는 today+1
        Set<LocalDate> savedDates = noteRepository.findSavedMeditationTimestamps(
                        memberId, windowStart.atStartOfDay(), today.plusDays(1).atStartOfDay())
                .stream()
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());

        // 오늘 저장됐으면 오늘부터, 아니면 어제부터 anchor (어제까지 연속 인정)
        LocalDate anchor = savedDates.contains(today) ? today : today.minusDays(1);

        long streak = 0;
        LocalDate cursor = anchor;
        while (!cursor.isBefore(windowStart) && savedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
