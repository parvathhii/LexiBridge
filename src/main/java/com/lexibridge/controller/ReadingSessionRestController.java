package com.lexibridge.controller;

import com.lexibridge.dto.ReadingSessionDto;
import com.lexibridge.entity.ReadingSession;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.repository.StudentProfileRepository;
import com.lexibridge.service.ReadingSessionService;
import com.lexibridge.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reading")
public class ReadingSessionRestController {

    private final ReadingSessionService readingSessionService;
    private final UserService userService;
    private final StudentProfileRepository studentProfileRepository;

    public ReadingSessionRestController(ReadingSessionService readingSessionService,
                                        UserService userService,
                                        StudentProfileRepository studentProfileRepository) {
        this.readingSessionService = readingSessionService;
        this.userService = userService;
        this.studentProfileRepository = studentProfileRepository;
    }

    @PostMapping("/session/complete")
    public ResponseEntity<Map<String, Object>> completeSession(@RequestBody ReadingSessionDto dto, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "User is not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.status(404).body(response);
        }

        try {
            ReadingSession session = readingSessionService.recordSession(user, dto);
            StudentProfile profile = studentProfileRepository.findByUser(user).orElse(null);

            response.put("success", true);
            response.put("sessionId", session.getId());
            response.put("wordsRead", session.getWordsRead());
            response.put("durationSeconds", session.getDurationSeconds());
            response.put("wpm", session.getWordsPerMinute());
            response.put("readingAccuracy", session.getReadingAccuracy());
            response.put("skippedWords", session.getSkippedWordCount());
            response.put("repeatedWords", session.getRepeatedWordCount());
            response.put("totalMinutes", profile != null ? profile.getTotalReadingMinutes() : 0);
            response.put("totalWords", profile != null ? profile.getWordsPracticedCount() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(@RequestBody Map<String, Object> prefs, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            return ResponseEntity.status(401).body(response);
        }

        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            response.put("success", false);
            return ResponseEntity.status(404).body(response);
        }

        studentProfileRepository.findByUser(user).ifPresent(profile -> {
            if (prefs.containsKey("font")) {
                profile.setFontPreference(prefs.get("font").toString());
            }
            if (prefs.containsKey("fontSize")) {
                profile.setFontSize(Integer.valueOf(prefs.get("fontSize").toString()));
            }
            if (prefs.containsKey("lineSpacing")) {
                profile.setLineSpacing(Double.valueOf(prefs.get("lineSpacing").toString()));
            }
            if (prefs.containsKey("theme")) {
                profile.setThemePreference(prefs.get("theme").toString());
            }
            if (prefs.containsKey("ruler")) {
                profile.setReadingRulerEnabled(Boolean.valueOf(prefs.get("ruler").toString()));
            }
            studentProfileRepository.save(profile);
        });

        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
