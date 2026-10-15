package com.lexibridge.controller;

import com.lexibridge.dto.GameAttemptDto;
import com.lexibridge.entity.GameAttempt;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.repository.StudentProfileRepository;
import com.lexibridge.service.GameService;
import com.lexibridge.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameRestController {

    private final GameService gameService;
    private final UserService userService;
    private final StudentProfileRepository studentProfileRepository;

    public GameRestController(GameService gameService,
                              UserService userService,
                              StudentProfileRepository studentProfileRepository) {
        this.gameService = gameService;
        this.userService = userService;
        this.studentProfileRepository = studentProfileRepository;
    }

    @PostMapping("/attempt")
    public ResponseEntity<Map<String, Object>> recordAttempt(@RequestBody GameAttemptDto dto, Principal principal) {
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

        try {
            GameAttempt attempt = gameService.recordGameAttempt(user, dto);
            StudentProfile profile = studentProfileRepository.findByUser(user).orElse(null);

            response.put("success", true);
            response.put("attemptId", attempt.getId());
            response.put("gameCode", attempt.getGameCode());
            response.put("score", attempt.getScore());
            response.put("accuracy", attempt.getAccuracy());
            response.put("totalWordsPracticed", profile != null ? profile.getWordsPracticedCount() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
