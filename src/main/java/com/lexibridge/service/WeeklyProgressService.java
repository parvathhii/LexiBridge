package com.lexibridge.service;

import com.lexibridge.entity.*;
import com.lexibridge.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WeeklyProgressService {
    private static final DateTimeFormatter RANGE_FORMAT = DateTimeFormatter.ofPattern("MMM d");

    private final ReadingSessionRepository readingSessionRepository;
    private final SpeechAttemptRepository speechAttemptRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final GameAttemptRepository gameAttemptRepository;
    private final ReadingErrorRepository readingErrorRepository;

    public WeeklyProgressService(ReadingSessionRepository readingSessionRepository,
                                 SpeechAttemptRepository speechAttemptRepository,
                                 QuizAttemptRepository quizAttemptRepository,
                                 GameAttemptRepository gameAttemptRepository,
                                 ReadingErrorRepository readingErrorRepository) {
        this.readingSessionRepository = readingSessionRepository;
        this.speechAttemptRepository = speechAttemptRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.gameAttemptRepository = gameAttemptRepository;
        this.readingErrorRepository = readingErrorRepository;
    }

    @Transactional(readOnly = true)
    public ProgressReport buildReport(User student) {
        LocalDate today = LocalDate.now();
        LocalDate firstWeek = today.with(DayOfWeek.MONDAY).minusWeeks(5);
        LocalDateTime rangeStart = firstWeek.atStartOfDay();

        List<ReadingSession> readings = readingSessionRepository.findByStudentOrderByCreatedAtDesc(student);
        List<SpeechAttempt> speeches = speechAttemptRepository.findByStudentOrderByCompletedAtDesc(student);
        List<QuizAttempt> quizzes = quizAttemptRepository.findByStudent(student);
        List<GameAttempt> games = gameAttemptRepository.findByStudentOrderByCompletedAtDesc(student);
        List<ReadingError> errors = readingErrorRepository.findByStudentOrderByCreatedAtDesc(student);

        List<WeekAccumulator> weeks = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) weeks.add(new WeekAccumulator(i, firstWeek.plusWeeks(i)));

        for (ReadingSession reading : readings) {
            int i = weekIndex(reading.getCreatedAt(), rangeStart, firstWeek);
            if (i >= 0) {
                weeks.get(i).addReading(reading);
            }
        }
        for (SpeechAttempt speech : speeches) {
            int i = weekIndex(speech.getCompletedAt(), rangeStart, firstWeek);
            if (i >= 0) {
                weeks.get(i).addSpeech(speech);
                if (speech.getAccuracy() != null && speech.getAccuracy() < 80) {
                    weeks.get(i).addDifficultWord(speech.getTargetWord(), 1);
                }
            }
        }
        for (QuizAttempt quiz : quizzes) {
            int i = weekIndex(quiz.getCompletedAt(), rangeStart, firstWeek);
            if (i >= 0) weeks.get(i).addQuiz(quiz);
        }
        for (GameAttempt game : games) {
            int i = weekIndex(game.getCompletedAt(), rangeStart, firstWeek);
            if (i >= 0) weeks.get(i).addGame();
        }
        for (ReadingError error : errors) {
            int i = weekIndex(error.getCreatedAt(), rangeStart, firstWeek);
            if (i >= 0) weeks.get(i).addDifficultWord(error.getPattern(), error.getOccurrences());
        }

        boolean hasActivity = weeks.stream().anyMatch(WeekAccumulator::hasActivity);
        if (!hasActivity) {
            for (int i = 0; i < weeks.size(); i++) weeks.set(i, sampleWeek(i, firstWeek.plusWeeks(i)));
        }

        List<WeekMetrics> metrics = new ArrayList<>(6);
        int maxSpeed = 1;
        int maxGames = 1;
        for (WeekAccumulator week : weeks) {
            maxSpeed = Math.max(maxSpeed, week.readingSpeed() == null ? 0 : week.readingSpeed());
            maxGames = Math.max(maxGames, week.games);
        }
        for (int i = 0; i < weeks.size(); i++) {
            WeekAccumulator current = weeks.get(i);
            WeekAccumulator previous = i == 0 ? null : weeks.get(i - 1);
            metrics.add(current.toMetrics(previous, maxSpeed, maxGames));
        }

        WeekMetrics latest = metrics.get(metrics.size() - 1);
        WeekMetrics previous = metrics.get(metrics.size() - 2);
        return new ProgressReport(student, !hasActivity, metrics, recommendations(latest, previous));
    }

    private int weekIndex(LocalDateTime dateTime, LocalDateTime start, LocalDate firstWeek) {
        if (dateTime == null || dateTime.isBefore(start)) return -1;
        long index = java.time.temporal.ChronoUnit.WEEKS.between(firstWeek, dateTime.toLocalDate().with(DayOfWeek.MONDAY));
        return index >= 0 && index < 6 ? (int) index : -1;
    }

    private WeekAccumulator sampleWeek(int index, LocalDate start) {
        int[][] sample = {
                {64, 43, 59, 56, 2}, {67, 47, 63, 61, 3}, {71, 50, 68, 65, 3},
                {74, 55, 72, 70, 4}, {79, 60, 77, 75, 5}, {84, 66, 82, 81, 6}
        };
        int[] values = sample[index];
        WeekAccumulator week = new WeekAccumulator(index, start);
        week.readingAccuracySum = values[0]; week.readingAccuracyCount = 1;
        week.readingSpeedSum = values[1]; week.readingSpeedCount = 1;
        week.speechSum = values[2]; week.speechCount = 1;
        week.quizSum = values[3]; week.quizCount = 1;
        week.games = values[4];
        if (index < 3) week.difficultWords.put("b/d", 3 - index);
        if (index < 2) week.difficultWords.put("skipped words", 2 - index);
        if (index == 4) week.difficultWords.put("friend", 1);
        return week;
    }

    private List<String> recommendations(WeekMetrics latest, WeekMetrics previous) {
        List<String> tips = new ArrayList<>();
        if (latest.readingAccuracy() != null && latest.readingAccuracy() < 80) {
            tips.add("Read a short familiar story together, then pause to sound out tricky words.");
        }
        if (latest.readingSpeed() != null && latest.readingSpeed() < 60) {
            tips.add("Build reading speed gently with a one-minute reread of a favorite passage.");
        }
        if (latest.speechScore() != null && latest.speechScore() < 75) {
            tips.add("Practice a few spoken words at a time and repeat them at a comfortable pace.");
        }
        if (latest.quizMark() != null && latest.quizMark() < 75) {
            tips.add("Review the story together before trying its quiz again.");
        }
        if (latest.gamesCompleted() < 3) {
            tips.add("Try two or three short word games this week, with breaks between rounds.");
        }
        if (!latest.difficultWords().isEmpty()) {
            tips.add("Give extra practice to: " + String.join(", ", latest.difficultWords()) + ".");
        }
        if (tips.isEmpty()) tips.add("Keep the routine steady and choose a slightly more challenging reading activity when the student feels ready.");
        if (latest.readingAccuracy() != null && previous.readingAccuracy() != null
                && latest.readingAccuracy() > previous.readingAccuracy()) {
            tips.add("Reading accuracy improved from last week. Celebrate the effort and keep practicing.");
        }
        return tips.stream().limit(4).toList();
    }

    private static int average(long sum, int count) {
        return count == 0 ? 0 : (int) Math.round((double) sum / count);
    }

    private static String change(Integer now, Integer before) {
        if (now == null || before == null) return "—";
        if (before == 0) return now == 0 ? "0%" : "+100%";
        int percent = (int) Math.round(((now - before) * 100.0) / before);
        return (percent > 0 ? "+" : "") + percent + "%";
    }

    private static int bar(int value, int max) {
        return max <= 0 ? 0 : Math.max(value > 0 ? 3 : 0, Math.min(100, (int) Math.round(value * 100.0 / max)));
    }

    public record ProgressReport(User student, boolean sampleData, List<WeekMetrics> weeks,
                                 List<String> recommendations) { }

    public record WeekMetrics(String weekLabel, String dateRange, Integer readingAccuracy,
                              Integer readingSpeed, Integer speechScore, Integer quizMark,
                              int gamesCompleted, List<String> difficultWords,
                              String readingAccuracyChange, String readingSpeedChange,
                              String speechScoreChange, String quizMarkChange, String gamesChange,
                              int readingAccuracyBar, int readingSpeedBar, int speechScoreBar,
                              int quizMarkBar, int gamesBar) { }

    private static class WeekAccumulator {
        private final int index;
        private final LocalDate start;
        private long readingAccuracySum, readingSpeedSum, speechSum, quizSum;
        private int readingAccuracyCount, readingSpeedCount, speechCount, quizCount, games, activityCount;
        private final Map<String, Integer> difficultWords = new HashMap<>();

        private WeekAccumulator(int index, LocalDate start) { this.index = index; this.start = start; }

        private void addReading(ReadingSession reading) {
            activityCount++;
            if (reading.getReadingAccuracy() != null) { readingAccuracySum += reading.getReadingAccuracy(); readingAccuracyCount++; }
            double wpm = reading.getWordsPerMinute() == null ? 0 : reading.getWordsPerMinute();
            if (wpm <= 0 && reading.getDurationSeconds() != null && reading.getDurationSeconds() > 0) {
                wpm = (reading.getWordsRead() == null ? 0 : reading.getWordsRead()) * 60.0 / reading.getDurationSeconds();
            }
            if (wpm > 0) { readingSpeedSum += Math.round(wpm); readingSpeedCount++; }
            int skipped = reading.getSkippedWordCount() == null ? 0 : reading.getSkippedWordCount();
            int repeated = reading.getRepeatedWordCount() == null ? 0 : reading.getRepeatedWordCount();
            if (skipped > 0) difficultWords.merge("skipped words", skipped, Integer::sum);
            if (repeated > 0) difficultWords.merge("repeated words", repeated, Integer::sum);
        }

        private void addSpeech(SpeechAttempt speech) {
            activityCount++;
            if (speech.getAccuracy() != null) { speechSum += speech.getAccuracy(); speechCount++; }
        }
        private void addQuiz(QuizAttempt quiz) {
            activityCount++;
            if (quiz.getPercentage() != null) { quizSum += Math.round(quiz.getPercentage()); quizCount++; }
        }
        private void addGame() { games++; activityCount++; }
        private void addDifficultWord(String word, Integer count) {
            if (word != null && !word.isBlank()) {
                difficultWords.merge(word.trim(), count == null ? 1 : Math.max(1, count), Integer::sum);
                activityCount++;
            }
        }
        private Integer readingAccuracy() { return readingAccuracyCount == 0 ? null : average(readingAccuracySum, readingAccuracyCount); }
        private Integer readingSpeed() { return readingSpeedCount == 0 ? null : average(readingSpeedSum, readingSpeedCount); }
        private Integer speechScore() { return speechCount == 0 ? null : average(speechSum, speechCount); }
        private Integer quizMark() { return quizCount == 0 ? null : average(quizSum, quizCount); }
        private boolean hasActivity() { return activityCount > 0; }

        private List<String> topDifficultWords() {
            return difficultWords.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5).map(entry -> entry.getKey() + " (" + entry.getValue() + ")").toList();
        }

        private WeekMetrics toMetrics(WeekAccumulator previous, int maxSpeed, int maxGames) {
            Integer accuracy = readingAccuracy(), speed = readingSpeed(), speech = speechScore(), quiz = quizMark();
            LocalDate end = start.plusDays(6).isAfter(LocalDate.now()) ? LocalDate.now() : start.plusDays(6);
            String range = start.format(RANGE_FORMAT) + "–" + end.format(RANGE_FORMAT);
            return new WeekMetrics("Week " + (index + 1), range, accuracy, speed, speech, quiz, games,
                    topDifficultWords(), change(accuracy, previous == null ? null : previous.readingAccuracy()),
                    change(speed, previous == null ? null : previous.readingSpeed()),
                    change(speech, previous == null ? null : previous.speechScore()),
                    change(quiz, previous == null ? null : previous.quizMark()),
                    previous == null ? "—" : change(games, previous.games),
                    accuracy == null ? 0 : accuracy, bar(speed == null ? 0 : speed, maxSpeed),
                    speech == null ? 0 : speech, quiz == null ? 0 : quiz, bar(games, maxGames));
        }
    }
}
