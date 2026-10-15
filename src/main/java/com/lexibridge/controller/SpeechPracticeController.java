package com.lexibridge.controller;

import com.lexibridge.dto.SpeechAttemptRequest;
import com.lexibridge.entity.SpeechAttempt;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.repository.SpeechAttemptRepository;
import com.lexibridge.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@Controller
public class SpeechPracticeController {
    private static final Map<Integer, List<String>> WORD_LEVELS = Map.of(
            1, List.of("bed", "bad", "bat", "bag", "big", "bid", "dig", "dog", "dip", "pad", "pat", "pig"),
            2, List.of("brag", "bring", "drip", "drum", "pocket", "puddle", "quick", "quack", "queen", "rabbit", "dentist", "birthday"),
            3, List.of("backpack", "bedroom", "dolphin", "dragon", "disappear", "playground", "pumpkin", "pudding", "problem", "question", "quilted", "paragraph"));
    private static final Map<Integer, String> LEVEL_FOCUS = Map.of(
            1, "Short practice words focus on b and d, p, and familiar sound contrasts.",
            2, "Build confidence with b and d blends, p words, and q-u words like quick and queen.",
            3, "Take on longer words with b, d, p, and q-u patterns. Listen for each sound as you speak.");
    private final UserService userService;
    private final SpeechAttemptRepository attempts;

    public SpeechPracticeController(UserService userService, SpeechAttemptRepository attempts) {
        this.userService = userService;
        this.attempts = attempts;
    }

    @GetMapping("/student/speech")
    public String practice(@RequestParam(value = "level", required = false) Integer requestedLevel, Principal principal, Model model) {
        User student = userService.findByUsername(principal.getName()).orElseThrow();
        StudentProfile profile = userService.getStudentProfile(student).orElse(null);
        int level = requestedLevel != null && requestedLevel >= 1 && requestedLevel <= 3
                ? requestedLevel : profile == null ? 1 : Math.min(3, Math.max(1, profile.getCurrentReadingLevel()));
        List<SpeechAttempt> history = attempts.findTop8ByStudentOrderByCompletedAtDesc(student);
        long count = attempts.countByStudent(student);
        int average = attempts.findByStudentOrderByCompletedAtDesc(student).stream()
                .mapToInt(SpeechAttempt::getAccuracy).sum();
        model.addAttribute("words", WORD_LEVELS.get(level));
        model.addAttribute("levelFocus", LEVEL_FOCUS.get(level));
        model.addAttribute("selectedLevel", level);
        model.addAttribute("attemptCount", count);
        model.addAttribute("averageAccuracy", count == 0 ? 0 : Math.round((double) average / count));
        model.addAttribute("history", history);
        return "student/speech-practice";
    }

    @PostMapping("/api/speech/attempt")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveAttempt(@RequestBody SpeechAttemptRequest request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("message", "Please sign in again."));
        String target = request.getTargetWord() == null ? "" : request.getTargetWord().trim().toLowerCase(Locale.ROOT);
        int level = request.getLevel() == null ? 0 : request.getLevel();
        String spoken = request.getRecognizedText() == null ? "" : request.getRecognizedText().trim();
        if (level < 1 || level > 3 || !WORD_LEVELS.get(level).contains(target) || spoken.isBlank() || spoken.length() > 160) {
            return ResponseEntity.badRequest().body(Map.of("message", "Choose a practice word and try speaking again."));
        }
        User student = userService.findByUsername(principal.getName()).orElseThrow();
        int accuracy = score(target, spoken.toLowerCase(Locale.ROOT));
        SpeechAttempt saved = attempts.save(new SpeechAttempt(student, target, spoken, accuracy, level));
        return ResponseEntity.ok(Map.of("id", saved.getId(), "accuracy", accuracy, "targetWord", target, "recognizedText", spoken, "level", level));
    }

    private int score(String expected, String actual) {
        int[] row = new int[actual.length() + 1];
        for (int j = 0; j <= actual.length(); j++) row[j] = j;
        for (int i = 1; i <= expected.length(); i++) {
            int diagonal = row[0]; row[0] = i;
            for (int j = 1; j <= actual.length(); j++) {
                int above = row[j];
                row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), diagonal + (expected.charAt(i - 1) == actual.charAt(j - 1) ? 0 : 1));
                diagonal = above;
            }
        }
        return Math.max(0, (int) Math.round(100.0 * (expected.length() - Math.min(expected.length(), row[actual.length()])) / expected.length()));
    }
}
