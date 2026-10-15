package com.lexibridge.controller;

import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.service.GameService;
import com.lexibridge.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequestMapping("/student/games")
public class GameController {

    private final UserService userService;
    private final GameService gameService;

    public GameController(UserService userService, GameService gameService) {
        this.userService = userService;
        this.gameService = gameService;
    }

    @GetMapping
    public String gamesHub(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        StudentProfile profile = user != null ? userService.getStudentProfile(user).orElse(null) : null;

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("totalAttempts", user != null ? gameService.countAttemptsByStudent(user) : 0);
        model.addAttribute("recommendedLevel", profile != null ? Math.min(3, Math.max(1, profile.getCurrentReadingLevel())) : 1);

        return "student/games/games-hub";
    }

    @GetMapping("/{slug}")
    public String playSpecificGame(@PathVariable("slug") String slug,
                                   @RequestParam(value = "level", required = false) Integer requestedLevel,
                                   Principal principal, Model model) {
        if ("hear-spell".equals(slug) || "listen-and-spell".equals(slug) || "sentence-puzzle".equals(slug)) {
            return "redirect:/student/games";
        }

        User user = userService.findByUsername(principal.getName()).orElse(null);
        StudentProfile profile = user != null ? userService.getStudentProfile(user).orElse(null) : null;

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("gameSlug", slug);
        int level = requestedLevel != null && requestedLevel >= 1 && requestedLevel <= 3
                ? requestedLevel : ("word-builder".equals(slug) || "syllable-switch".equals(slug)) ? 3
                : profile == null ? 1 : Math.min(3, Math.max(1, profile.getCurrentReadingLevel()));
        model.addAttribute("gameLevel", level);
        model.addAttribute("recommendedLevel", profile != null ? Math.min(3, Math.max(1, profile.getCurrentReadingLevel())) : 1);

        return "student/games/game-arena";
    }
}
