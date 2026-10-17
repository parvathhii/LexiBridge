package com.lexibridge.controller;

import com.lexibridge.entity.*;
import com.lexibridge.repository.*;
import com.lexibridge.service.UserService;
import com.lexibridge.service.ParentStudentLinkService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/parent")
public class ParentController {
    private final UserService userService;
    private final ParentStudentRepository parentStudentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final ReadingErrorRepository readingErrorRepository;
    private final SpeechAttemptRepository speechAttemptRepository;
    private final GameAttemptRepository gameAttemptRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ParentReminderRepository parentReminderRepository;
    private final ParentStudentLinkService parentStudentLinkService;

    public ParentController(UserService userService, ParentStudentRepository parentStudentRepository,
                            StudentProfileRepository studentProfileRepository,
                            ReadingSessionRepository readingSessionRepository,
                            ReadingErrorRepository readingErrorRepository,
                            SpeechAttemptRepository speechAttemptRepository,
                            GameAttemptRepository gameAttemptRepository,
                            QuizAttemptRepository quizAttemptRepository,
                            ParentReminderRepository parentReminderRepository,
                            ParentStudentLinkService parentStudentLinkService) {
        this.userService = userService;
        this.parentStudentRepository = parentStudentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.readingSessionRepository = readingSessionRepository;
        this.readingErrorRepository = readingErrorRepository;
        this.speechAttemptRepository = speechAttemptRepository;
        this.gameAttemptRepository = gameAttemptRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.parentReminderRepository = parentReminderRepository;
        this.parentStudentLinkService = parentStudentLinkService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "childId", required = false) Long childId,
                            @RequestParam(value = "activityPeriod", defaultValue = "today") String requestedActivityPeriod,
                            Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        User parent = userService.findByUsername(principal.getName()).orElse(null);
        if (parent == null) return "redirect:/login";
        String activityPeriod = List.of("today", "yesterday", "this-week").contains(requestedActivityPeriod)
                ? requestedActivityPeriod : "today";
        LocalDate activityStart = switch (activityPeriod) {
            case "today" -> LocalDate.now();
            case "yesterday" -> LocalDate.now().minusDays(1);
            default -> LocalDate.now().with(DayOfWeek.MONDAY);
        };
        LocalDate activityEnd = switch (activityPeriod) {
            case "today" -> activityStart.plusDays(1);
            case "yesterday" -> activityStart.plusDays(1);
            default -> activityStart.plusWeeks(1);
        };
        model.addAttribute("activityPeriod", activityPeriod);

        List<ParentStudent> links = parentStudentRepository.findByParent(parent);
        List<User> linkedChildren = links.stream().map(ParentStudent::getStudent).toList();
        User selectedChild = linkedChildren.stream().filter(c -> Objects.equals(c.getId(), childId)).findFirst()
                .orElse(linkedChildren.isEmpty() ? null : linkedChildren.get(0));
        List<Map<String, Object>> childCards = new ArrayList<>();
        int allMinutes = 0, allWords = 0;
        for (User child : linkedChildren) {
            StudentProfile profile = studentProfileRepository.findByUser(child).orElse(null);
            List<ReadingSession> reading = readingSessionRepository.findByStudentOrderByCreatedAtDesc(child);
            int minutes = (int) reading.stream().mapToInt(s -> safe(s.getDurationSeconds())).sum() / 60;
            int words = reading.stream().mapToInt(s -> safe(s.getWordsRead())).sum();
            int accuracy = average(reading.stream().map(ReadingSession::getReadingAccuracy).filter(Objects::nonNull).toList(),
                    speechAttemptRepository.findByStudentOrderByCompletedAtDesc(child).stream().map(SpeechAttempt::getAccuracy).toList());
            childCards.add(Map.of("student", child, "minutes", minutes, "words", words,
                    "accuracy", accuracy, "readingLevel", profile == null ? 1 : safe(profile.getCurrentReadingLevel()),
                    "selected", selectedChild != null && Objects.equals(selectedChild.getId(), child.getId())));
            allMinutes += minutes;
            allWords += words;
        }

        model.addAttribute("parent", parent);
        model.addAttribute("children", childCards);
        model.addAttribute("childrenCount", childCards.size());
        model.addAttribute("totalMinutes", allMinutes);
        model.addAttribute("totalWords", allWords);
        model.addAttribute("selectedChild", selectedChild);
        model.addAttribute("childId", selectedChild == null ? null : selectedChild.getId());
        if (selectedChild == null) {
            model.addAttribute("hasChild", false);
            model.addAttribute("reminders", List.of());
            model.addAttribute("weeklyProgress", List.of());
            model.addAttribute("recentReading", List.of());
            model.addAttribute("recentSpeech", List.of());
            model.addAttribute("recentGames", List.of());
            model.addAttribute("recentQuizzes", List.of());
            model.addAttribute("difficultWords", List.of());
            model.addAttribute("letterPatterns", List.of());
            model.addAttribute("recommendations", List.of("Ask the school administrator to link your account to a student profile."));
            model.addAttribute("achievements", List.of());
            return "parent/dashboard";
        }
        model.addAttribute("hasChild", true);

        User child = selectedChild;
        StudentProfile profile = studentProfileRepository.findByUser(child).orElse(null);
        List<ReadingSession> reading = readingSessionRepository.findByStudentOrderByCreatedAtDesc(child);
        List<SpeechAttempt> speech = speechAttemptRepository.findByStudentOrderByCompletedAtDesc(child);
        List<GameAttempt> games = gameAttemptRepository.findByStudentOrderByCompletedAtDesc(child);
        List<QuizAttempt> quizzes = quizAttemptRepository.findByStudent(child);
        List<ReadingError> errors = readingErrorRepository.findByStudentOrderByCreatedAtDesc(child);
        List<ReadingSession> recentReading = reading.stream()
                .filter(entry -> inActivityRange(entry.getCreatedAt(), activityStart, activityEnd)).limit(10).toList();
        List<SpeechAttempt> recentSpeech = speech.stream()
                .filter(entry -> inActivityRange(entry.getCompletedAt(), activityStart, activityEnd)).limit(10).toList();
        List<GameAttempt> recentGames = games.stream()
                .filter(entry -> inActivityRange(entry.getCompletedAt(), activityStart, activityEnd)).limit(10).toList();
        List<QuizAttempt> recentQuizzes = quizzes.stream()
                .filter(entry -> inActivityRange(entry.getCompletedAt(), activityStart, activityEnd))
                .sorted(Comparator.comparing(QuizAttempt::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10).toList();
        List<ParentReminder> reminders = parentReminderRepository.findByParentAndStudentOrderByDueDateAsc(parent, child);

        List<Integer> readingScores = reading.stream().map(ReadingSession::getReadingAccuracy).filter(Objects::nonNull).toList();
        List<Integer> speechScores = speech.stream().map(SpeechAttempt::getAccuracy).toList();
        int readingAccuracy = average(readingScores);
        int speechAccuracy = average(speechScores);
        int gameAccuracy = average(games.stream().map(g -> (int) Math.round(g.getAccuracy())).toList());
        int quizAccuracy = quizzes.isEmpty() ? 0 : (int) Math.round(quizzes.stream().mapToDouble(QuizAttempt::getPercentage).average().orElse(0));
        int totalReadingMinutes = (int) reading.stream().mapToInt(s -> safe(s.getDurationSeconds())).sum() / 60;
        int gameMinutes = (int) games.stream().mapToInt(g -> safe(g.getTimeSpentSeconds())).sum() / 60;
        int learningMinutes = totalReadingMinutes + gameMinutes;
        int quizCompleted = quizzes.size();

        Map<String, Integer> wordCounts = new HashMap<>();
        Map<String, Integer> pairCounts = new HashMap<>();
        for (ReadingError error : errors) {
            if (List.of("SKIPPED_WORD", "DIFFICULT_WORD", "REPEATED_WORD").contains(error.getErrorType()))
                wordCounts.merge(error.getPattern(), safe(error.getOccurrences()), Integer::sum);
            if ("LETTER_CONFUSION".equals(error.getErrorType()))
                pairCounts.merge(error.getPattern(), safe(error.getOccurrences()), Integer::sum);
        }
        speech.stream().filter(a -> a.getAccuracy() < 100).forEach(a -> wordCounts.merge(a.getTargetWord(), 1, Integer::sum));
        List<Map<String, Object>> difficultWords = wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(5).map(e -> Map.<String, Object>of("word", e.getKey(), "count", e.getValue())).toList();
        List<Map<String, Object>> letterPatterns = pairCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> Map.<String, Object>of("pattern", e.getKey(), "count", e.getValue())).toList();

        List<Map<String, Object>> weeklyProgress = weeklyProgress(reading, speech, games, quizzes);
        int maxWeeklyMinutes = weeklyProgress.stream().mapToInt(w -> (Integer) w.get("minutes")).max().orElse(1);
        model.addAttribute("profile", profile);
        model.addAttribute("readingAccuracy", readingAccuracy);
        model.addAttribute("speechAccuracy", speechAccuracy);
        model.addAttribute("gameAccuracy", gameAccuracy);
        model.addAttribute("quizAccuracy", quizAccuracy);
        model.addAttribute("totalReadingMinutes", totalReadingMinutes);
        model.addAttribute("gameMinutes", gameMinutes);
        model.addAttribute("learningMinutes", learningMinutes);
        model.addAttribute("readingSessions", reading.size());
        model.addAttribute("speechAttempts", speech.size());
        model.addAttribute("gamesCompleted", games.size());
        model.addAttribute("quizzesCompleted", quizCompleted);
        model.addAttribute("recentReading", recentReading);
        model.addAttribute("recentSpeech", recentSpeech);
        model.addAttribute("recentGames", recentGames);
        model.addAttribute("recentQuizzes", recentQuizzes);
        model.addAttribute("difficultWords", difficultWords);
        model.addAttribute("letterPatterns", letterPatterns);
        model.addAttribute("weeklyProgress", weeklyProgress);
        model.addAttribute("maxWeeklyMinutes", Math.max(maxWeeklyMinutes, 1));
        model.addAttribute("reminders", reminders.stream().map(r -> Map.<String, Object>of(
                "id", r.getId(), "title", r.getTitle(), "note", r.getNote(), "dueDate", r.getDueDate(),
                "completed", r.isCompleted(), "overdue", !r.isCompleted() && r.getDueDate().isBefore(LocalDate.now()))).toList());
        model.addAttribute("recommendations", recommendations(difficultWords, letterPatterns, speechAccuracy));
        model.addAttribute("achievements", achievements(reading.size(), games.size(), quizCompleted, speech.size(), learningMinutes));
        model.addAttribute("hasReadingAccuracy", !readingScores.isEmpty());
        model.addAttribute("hasSpeechAccuracy", !speechScores.isEmpty());
        model.addAttribute("hasGameAccuracy", !games.isEmpty());
        model.addAttribute("hasQuizAccuracy", !quizzes.isEmpty());
        return "parent/dashboard";
    }

    private boolean inActivityRange(LocalDateTime completedAt, LocalDate start, LocalDate endExclusive) {
        return completedAt != null && !completedAt.toLocalDate().isBefore(start)
                && completedAt.toLocalDate().isBefore(endExclusive);
    }

    @PostMapping("/link-student")
    public String linkStudent(@RequestParam String studentLinkCode, Principal principal, RedirectAttributes flash) {
        if (principal == null) return "redirect:/login";
        User parent = userService.findByUsername(principal.getName()).orElse(null);
        if (parent == null || parent.getRole() != Role.ROLE_PARENT) return "redirect:/";
        try {
            parentStudentLinkService.linkUsingCode(parent, studentLinkCode);
            flash.addFlashAttribute("parentNotice", "Student linked. Their learning progress is now available to this account.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("parentError", ex.getMessage());
        }
        return "redirect:/parent/dashboard";
    }

    @PostMapping("/children/{childId}/reading-settings")
    public String saveReadingSettings(@PathVariable Long childId,
                                      @RequestParam String fontPreference,
                                      @RequestParam Integer fontSize,
                                      @RequestParam Double lineSpacing,
                                      @RequestParam String letterSpacing,
                                      @RequestParam String readingTextColor,
                                      @RequestParam String themePreference,
                                      @RequestParam(defaultValue = "false") boolean readingRulerEnabled,
                                      Principal principal, RedirectAttributes flash) {
        if (principal == null) return "redirect:/login";
        User parent = userService.findByUsername(principal.getName()).orElse(null);
        if (parent == null || parent.getRole() != Role.ROLE_PARENT) return "redirect:/";
        User child = linkedChild(parent, childId);
        if (child == null) {
            flash.addFlashAttribute("parentError", "That child is not linked to your account.");
            return "redirect:/parent/dashboard";
        }

        Set<String> fonts = Set.of("OpenDyslexic", "Lexend", "Comic Neue", "Arial");
        Set<String> themes = Set.of("theme-offwhite", "theme-cream", "theme-lavender", "theme-sky", "theme-peach", "theme-dark");
        Set<String> spacings = Set.of("0.02em", "0.06em", "0.12em");
        Set<String> textColors = Set.of("#26364a", "#17324d", "#4b3621", "#f1f5f9");
        if (!fonts.contains(fontPreference) || fontSize == null || fontSize < 16 || fontSize > 32
                || lineSpacing == null || lineSpacing < 1.4 || lineSpacing > 2.6
                || !spacings.contains(letterSpacing) || !textColors.contains(readingTextColor)
                || !themes.contains(themePreference)
                || ("theme-dark".equals(themePreference) != "#f1f5f9".equals(readingTextColor))) {
            flash.addFlashAttribute("parentError", "Please choose valid reading settings and try again.");
            return "redirect:/parent/dashboard?childId=" + childId + "#reading-settings";
        }

        StudentProfile profile = studentProfileRepository.findByUser(child).orElseGet(() -> {
            StudentProfile created = new StudentProfile(child);
            created.setGradeLevel("Grade 3");
            created.setCurrentReadingLevel(1);
            return created;
        });
        profile.setFontPreference(fontPreference);
        profile.setFontSize(fontSize);
        profile.setLineSpacing(lineSpacing);
        profile.setLetterSpacing(letterSpacing);
        profile.setReadingTextColor(readingTextColor);
        profile.setThemePreference(themePreference);
        profile.setReadingRulerEnabled(readingRulerEnabled);
        studentProfileRepository.save(profile);
        flash.addFlashAttribute("parentNotice", "Reading preferences saved for " + child.getFullName() + ".");
        return "redirect:/parent/dashboard?childId=" + childId + "#reading-settings";
    }

    @PostMapping("/children/{childId}/reminders")
    public String createReminder(@PathVariable Long childId, @RequestParam String title,
                                 @RequestParam(required = false) String note,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                                 Principal principal, RedirectAttributes flash) {
        User parent = userService.findByUsername(principal.getName()).orElseThrow();
        User child = linkedChild(parent, childId);
        if (child == null) {
            flash.addFlashAttribute("parentError", "That child is not linked to your account.");
            return "redirect:/parent/dashboard";
        }
        String cleanTitle = title == null ? "" : title.trim();
        String cleanNote = note == null ? "" : note.trim();
        if (cleanTitle.isBlank() || cleanTitle.length() > 120 || cleanNote.length() > 500 || dueDate == null) {
            flash.addFlashAttribute("parentError", "Add a title (up to 120 characters), a valid due date, and a note under 500 characters.");
        } else {
            parentReminderRepository.save(new ParentReminder(parent, child, cleanTitle, cleanNote, dueDate));
            flash.addFlashAttribute("parentNotice", "Homework reminder saved.");
        }
        return "redirect:/parent/dashboard?childId=" + childId;
    }

    @PostMapping("/reminders/{reminderId}/complete")
    public String completeReminder(@PathVariable Long reminderId, @RequestParam boolean completed,
                                   Principal principal, RedirectAttributes flash) {
        User parent = userService.findByUsername(principal.getName()).orElseThrow();
        ParentReminder reminder = parentReminderRepository.findByIdAndParent(reminderId, parent).orElse(null);
        if (reminder == null || linkedChild(parent, reminder.getStudent().getId()) == null) {
            flash.addFlashAttribute("parentError", "That reminder is not available to your account.");
            return "redirect:/parent/dashboard";
        }
        reminder.setCompleted(completed);
        parentReminderRepository.save(reminder);
        flash.addFlashAttribute("parentNotice", completed ? "Reminder marked complete." : "Reminder reopened.");
        return "redirect:/parent/dashboard?childId=" + reminder.getStudent().getId();
    }

    private User linkedChild(User parent, Long childId) {
        return parentStudentRepository.findByParent(parent).stream().map(ParentStudent::getStudent)
                .filter(c -> Objects.equals(c.getId(), childId)).findFirst().orElse(null);
    }

    private int safe(Integer value) { return value == null ? 0 : value; }
    private int average(List<Integer>... groups) {
        List<Integer> values = Arrays.stream(groups).flatMap(Collection::stream).filter(Objects::nonNull).toList();
        return average(values);
    }
    private int average(List<Integer> values) {
        return values.isEmpty() ? 0 : (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private List<Map<String, Object>> weeklyProgress(List<ReadingSession> reading, List<SpeechAttempt> speech,
                                                     List<GameAttempt> games, List<QuizAttempt> quizzes) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Map<String, Object>> weeks = new ArrayList<>();
        for (int offset = 7; offset >= 0; offset--) {
            LocalDate start = monday.minusWeeks(offset);
            LocalDateTime from = start.atStartOfDay(), to = start.plusWeeks(1).atStartOfDay();
            List<Integer> scores = new ArrayList<>();
            int minutes = 0, activities = 0;
            for (ReadingSession session : reading) if (inRange(session.getCreatedAt(), from, to)) {
                minutes += safe(session.getDurationSeconds()) / 60;
                if (session.getReadingAccuracy() != null) scores.add(session.getReadingAccuracy());
                activities++;
            }
            for (SpeechAttempt attempt : speech) if (inRange(attempt.getCompletedAt(), from, to)) { scores.add(attempt.getAccuracy()); activities++; }
            for (GameAttempt attempt : games) if (inRange(attempt.getCompletedAt(), from, to)) {
                minutes += safe(attempt.getTimeSpentSeconds()) / 60; scores.add((int) Math.round(attempt.getAccuracy())); activities++;
            }
            for (QuizAttempt attempt : quizzes) if (inRange(attempt.getCompletedAt(), from, to)) { scores.add((int) Math.round(attempt.getPercentage())); activities++; }
            weeks.add(Map.of("label", start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + start.getDayOfMonth(),
                    "minutes", minutes, "accuracy", average(scores), "activities", activities));
        }
        return weeks;
    }
    private boolean inRange(LocalDateTime date, LocalDateTime from, LocalDateTime to) { return date != null && !date.isBefore(from) && date.isBefore(to); }

    private List<String> recommendations(List<Map<String, Object>> words, List<Map<String, Object>> pairs, int speechAccuracy) {
        List<String> tips = new ArrayList<>();
        if (!pairs.isEmpty()) tips.add("Try a short Letter Detective round for " + pairs.get(0).get("pattern") + " and celebrate careful attempts.");
        if (!words.isEmpty()) tips.add("Practice breaking '" + words.get(0).get("word") + "' into syllables with Syllable Switch, then find it in a story.");
        if (speechAccuracy > 0 && speechAccuracy < 80) tips.add("Keep speech practice sessions short and repeat a familiar word set before moving to a harder level.");
        if (tips.isEmpty()) tips.add("Choose a short reading or game activity together and keep a comfortable, steady routine.");
        return tips;
    }

    private List<Map<String, Object>> achievements(int reading, int games, int quizzes, int speech, int minutes) {
        List<Map<String, Object>> badges = new ArrayList<>();
        badges.add(badge("First story", "Complete a Smart Reading session", reading >= 1));
        badges.add(badge("Reading rhythm", "Complete 5 reading sessions", reading >= 5));
        badges.add(badge("Game explorer", "Complete 3 learning games", games >= 3));
        badges.add(badge("Quiz finisher", "Complete a reading quiz", quizzes >= 1));
        badges.add(badge("Brave speaker", "Try Speech Practice", speech >= 1));
        badges.add(badge("Practice time", "Reach 30 minutes of reading and games", minutes >= 30));
        return badges;
    }
    private Map<String, Object> badge(String title, String description, boolean earned) {
        return Map.of("title", title, "description", description, "earned", earned);
    }
}
