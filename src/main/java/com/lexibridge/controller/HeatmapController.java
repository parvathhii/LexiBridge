package com.lexibridge.controller;

import com.lexibridge.entity.ReadingError;
import com.lexibridge.entity.ReadingSession;
import com.lexibridge.entity.User;
import com.lexibridge.repository.ReadingErrorRepository;
import com.lexibridge.repository.ReadingSessionRepository;
import com.lexibridge.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HeatmapController {
    private static final List<String> LETTER_PAIRS = List.of("b/d", "p/q", "m/w");
    private final UserService userService;
    private final ReadingSessionRepository sessions;
    private final ReadingErrorRepository errors;

    public HeatmapController(UserService userService, ReadingSessionRepository sessions, ReadingErrorRepository errors) {
        this.userService = userService;
        this.sessions = sessions;
        this.errors = errors;
    }

    @GetMapping("/student/heatmap")
    public String heatmap(Principal principal, Model model) {
        User student = userService.findByUsername(principal.getName()).orElseThrow();
        List<ReadingSession> allSessions = sessions.findByStudentOrderByCreatedAtDesc(student);
        List<ReadingSession> analyzed = allSessions.stream().filter(s -> s.getReadingAccuracy() != null).toList();
        List<ReadingError> readingErrors = errors.findByStudentOrderByCreatedAtDesc(student);

        Map<String, Integer> letters = new HashMap<>();
        Map<String, Integer> difficultWords = new HashMap<>();
        for (ReadingError error : readingErrors) {
            if ("LETTER_CONFUSION".equals(error.getErrorType())) letters.merge(error.getPattern(), error.getOccurrences(), Integer::sum);
            if (List.of("SKIPPED_WORD", "DIFFICULT_WORD", "REPEATED_WORD").contains(error.getErrorType()))
                difficultWords.merge(error.getPattern(), error.getOccurrences(), Integer::sum);
        }

        int accuracySum = analyzed.stream().mapToInt(ReadingSession::getReadingAccuracy).sum();
        int skipped = analyzed.stream().mapToInt(ReadingSession::getSkippedWordCount).sum();
        int repeated = analyzed.stream().mapToInt(ReadingSession::getRepeatedWordCount).sum();
        int averageAccuracy = analyzed.isEmpty() ? 0 : Math.round((float) accuracySum / analyzed.size());

        List<Map<String, Object>> letterCells = LETTER_PAIRS.stream().map(pair -> {
            int count = letters.getOrDefault(pair, 0);
            int max = LETTER_PAIRS.stream().mapToInt(p -> letters.getOrDefault(p, 0)).max().orElse(0);
            int intensity = count == 0 ? 0 : max == 0 ? 0 : Math.min(4, 1 + (count * 3 / max));
            return Map.<String, Object>of("pattern", pair, "count", count, "intensity", intensity,
                    "description", pair.replace('/', ' '));
        }).toList();
        List<Map<String, Object>> wordCells = difficultWords.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(8).map(e -> Map.<String, Object>of("word", e.getKey(), "count", e.getValue()))
                .toList();

        LocalDate thisWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Map<String, Object>> progress = new ArrayList<>();
        for (int offset = 7; offset >= 0; offset--) {
            LocalDate weekStart = thisWeek.minusWeeks(offset);
            LocalDateTime from = weekStart.atStartOfDay();
            LocalDateTime until = weekStart.plusWeeks(1).atStartOfDay();
            List<ReadingSession> weekSessions = analyzed.stream().filter(s -> !s.getCreatedAt().isBefore(from) && s.getCreatedAt().isBefore(until)).toList();
            int weekAccuracy = weekSessions.isEmpty() ? 0 : Math.round((float) weekSessions.stream().mapToInt(ReadingSession::getReadingAccuracy).sum() / weekSessions.size());
            progress.add(Map.of("label", weekStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + weekStart.getDayOfMonth(),
                    "accuracy", weekAccuracy, "sessions", weekSessions.size()));
        }

        List<String> recommendations = recommendations(letterCells, wordCells, skipped, repeated, analyzed.size());
        model.addAttribute("letterCells", letterCells);
        model.addAttribute("wordCells", wordCells);
        model.addAttribute("progress", progress);
        model.addAttribute("averageAccuracy", averageAccuracy);
        model.addAttribute("analyzedSessions", analyzed.size());
        model.addAttribute("totalSessions", allSessions.size());
        model.addAttribute("skippedWords", skipped);
        model.addAttribute("repeatedWords", repeated);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("hasAnalyzedData", !analyzed.isEmpty());
        return "student/heatmap";
    }

    private List<String> recommendations(List<Map<String, Object>> letters, List<Map<String, Object>> words,
                                          int skipped, int repeated, int sessionCount) {
        List<String> suggestions = new ArrayList<>();
        if (sessionCount == 0) {
            suggestions.add("Try a Smart Reading session with the optional microphone tracker to build your first reading pattern.");
            suggestions.add("Use the Read Aloud button at a comfortable pace; you can stop and retry at any time.");
            return suggestions;
        }
        Map<String, Object> topPair = letters.stream().max(Comparator.comparingInt(m -> (Integer) m.get("count"))).orElse(Map.of());
        if (!topPair.isEmpty() && (Integer) topPair.get("count") > 0)
            suggestions.add("Practice the letter pair " + topPair.get("pattern") + " with Letter Detective, then read a short passage aloud.");
        if (!words.isEmpty()) suggestions.add("Revisit the word '" + words.get(0).get("word") + "' by breaking it into syllables in Syllable Switch, then read it in a sentence.");
        if (skipped > 0) suggestions.add("Try following one line at a time with the reading ruler to support steady word-by-word reading.");
        if (repeated > 0) suggestions.add("Pause briefly at punctuation and reread the sentence once before moving to the next line.");
        if (suggestions.isEmpty()) suggestions.add("Keep practicing with passages at your current reading level to build a useful progress trend.");
        return suggestions;
    }
}
