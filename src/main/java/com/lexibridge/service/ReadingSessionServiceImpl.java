package com.lexibridge.service;

import com.lexibridge.dto.ReadingSessionDto;
import com.lexibridge.entity.ReadingMaterial;
import com.lexibridge.entity.ReadingSession;
import com.lexibridge.entity.ReadingError;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.repository.ReadingMaterialRepository;
import com.lexibridge.repository.ReadingSessionRepository;
import com.lexibridge.repository.ReadingErrorRepository;
import com.lexibridge.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ReadingSessionServiceImpl implements ReadingSessionService {

    private final ReadingSessionRepository readingSessionRepository;
    private final ReadingMaterialRepository readingMaterialRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ReadingErrorRepository readingErrorRepository;

    public ReadingSessionServiceImpl(ReadingSessionRepository readingSessionRepository,
                                     ReadingMaterialRepository readingMaterialRepository,
                                     StudentProfileRepository studentProfileRepository,
                                     ReadingErrorRepository readingErrorRepository) {
        this.readingSessionRepository = readingSessionRepository;
        this.readingMaterialRepository = readingMaterialRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.readingErrorRepository = readingErrorRepository;
    }

    @Override
    @Transactional
    public ReadingSession recordSession(User student, ReadingSessionDto dto) {
        ReadingMaterial material = readingMaterialRepository.findById(dto.getMaterialId())
                .orElseThrow(() -> new IllegalArgumentException("Material not found with ID: " + dto.getMaterialId()));

        ReadingSession session = new ReadingSession();
        session.setStudent(student);
        session.setMaterial(material);
        session.setDurationSeconds(dto.getDurationSeconds() != null ? dto.getDurationSeconds() : 0);
        session.setWordsRead(dto.getWordsRead() != null ? dto.getWordsRead() : material.getWordCount());
        session.setWordsPerMinute(dto.getWordsPerMinute() != null ? dto.getWordsPerMinute() : 0.0);
        session.setTtsUsed(dto.getTtsUsed() != null ? dto.getTtsUsed() : false);
        session.setCompleted(dto.getCompleted() != null ? dto.getCompleted() : true);

        String spokenText = dto.getSpokenText() == null ? "" : dto.getSpokenText().trim();
        if (spokenText.length() > 10000) throw new IllegalArgumentException("Speech sample is too long.");
        Map<String, Integer> errors = new LinkedHashMap<>();
        if (!spokenText.isBlank()) {
            List<String> expected = words(material.getContent());
            List<String> spoken = words(spokenText);
            int[] counts = analyze(expected, spoken, errors);
            session.setReadingAccuracy(expected.isEmpty() ? 0 : Math.max(0, Math.round(100f * counts[0] / expected.size())));
            session.setSkippedWordCount(counts[1]);
            session.setRepeatedWordCount(counts[2]);
        }

        ReadingSession savedSession = readingSessionRepository.save(session);
        List<ReadingError> savedErrors = new ArrayList<>();
        errors.forEach((key, count) -> {
            int split = key.indexOf('|');
            savedErrors.add(new ReadingError(student, savedSession,
                    key.substring(0, split), key.substring(split + 1), count));
        });
        if (!savedErrors.isEmpty()) readingErrorRepository.saveAll(savedErrors);

        // Update student profile totals
        studentProfileRepository.findByUser(student).ifPresent(profile -> {
            int sessionMinutes = Math.max(1, (dto.getDurationSeconds() != null ? dto.getDurationSeconds() : 60) / 60);
            profile.setTotalReadingMinutes(profile.getTotalReadingMinutes() + sessionMinutes);
            profile.setWordsPracticedCount(profile.getWordsPracticedCount() + session.getWordsRead());
            studentProfileRepository.save(profile);
        });

        return savedSession;
    }

    private List<String> words(String text) {
        List<String> result = new ArrayList<>();
        var matcher = Pattern.compile("[\\p{L}']+").matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) result.add(matcher.group().replaceAll("^'+|'+$", ""));
        result.removeIf(String::isBlank);
        return result;
    }

    /** Word-level edit distance. Exact matches count toward accuracy; deletions, insertions, and substitutions are saved as learning signals. */
    private int[] analyze(List<String> expected, List<String> spoken, Map<String, Integer> errors) {
        int n = expected.size(), m = spoken.size();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;
        for (int i = 1; i <= n; i++) for (int j = 1; j <= m; j++)
            dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + (expected.get(i - 1).equals(spoken.get(j - 1)) ? 0 : 1));

        int i = n, j = m, correct = 0, skipped = 0, repeated = 0;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && expected.get(i - 1).equals(spoken.get(j - 1)) && dp[i][j] == dp[i - 1][j - 1]) {
                correct++; i--; j--; continue;
            }
            if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + 1) {
                String target = expected.get(i - 1), heard = spoken.get(j - 1);
                addError(errors, "DIFFICULT_WORD", target);
                String confusion = confusion(target, heard);
                if (confusion != null) addError(errors, "LETTER_CONFUSION", confusion);
                i--; j--; continue;
            }
            if (i > 0 && dp[i][j] == dp[i - 1][j] + 1) {
                String missed = expected.get(i - 1);
                addError(errors, "SKIPPED_WORD", missed); skipped++; i--; continue;
            }
            if (j > 0) {
                String extra = spoken.get(j - 1);
                addError(errors, "REPEATED_WORD", extra); repeated++; j--; continue;
            }
        }
        return new int[]{correct, skipped, repeated};
    }

    private void addError(Map<String, Integer> errors, String type, String pattern) {
        errors.merge(type + "|" + pattern, 1, Integer::sum);
    }

    private String confusion(String expected, String heard) {
        if (expected.length() != heard.length()) return null;
        String[][] pairs = {{"b", "d"}, {"p", "q"}, {"m", "w"}};
        String found = null;
        for (int k = 0; k < expected.length(); k++) {
            if (expected.charAt(k) == heard.charAt(k)) continue;
            boolean isKnownPair = false;
            for (String[] pair : pairs) if ((expected.charAt(k) == pair[0].charAt(0) && heard.charAt(k) == pair[1].charAt(0)) || (expected.charAt(k) == pair[1].charAt(0) && heard.charAt(k) == pair[0].charAt(0))) {
                found = pair[0] + "/" + pair[1]; isKnownPair = true; break;
            }
            if (!isKnownPair || found != null && !found.equals(pairFor(expected.charAt(k), heard.charAt(k)))) return null;
        }
        return found;
    }

    private String pairFor(char first, char second) {
        return (first == 'b' || first == 'd') && (second == 'b' || second == 'd') ? "b/d"
                : (first == 'p' || first == 'q') && (second == 'p' || second == 'q') ? "p/q" : "m/w";
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingSession> getSessionsByStudent(User student) {
        return readingSessionRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSessionsByStudent(User student) {
        return readingSessionRepository.countByStudent(student);
    }
}
