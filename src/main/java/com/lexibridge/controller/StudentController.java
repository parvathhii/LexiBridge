package com.lexibridge.controller;

import com.lexibridge.entity.ReadingMaterial;
import com.lexibridge.entity.ReadingSession;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.service.QuizService;
import com.lexibridge.service.ReadingMaterialService;
import com.lexibridge.service.ReadingSessionService;
import com.lexibridge.service.UserService;
import com.lexibridge.repository.ParentStudentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final UserService userService;
    private final ReadingMaterialService readingMaterialService;
    private final ReadingSessionService readingSessionService;
    private final QuizService quizService;
    private final ParentStudentRepository parentStudentRepository;

    public StudentController(UserService userService,
                             ReadingMaterialService readingMaterialService,
                             ReadingSessionService readingSessionService,
                             QuizService quizService,
                             ParentStudentRepository parentStudentRepository) {
        this.userService = userService;
        this.readingMaterialService = readingMaterialService;
        this.readingSessionService = readingSessionService;
        this.quizService = quizService;
        this.parentStudentRepository = parentStudentRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        StudentProfile profile = userService.getStudentProfile(user).orElseGet(() -> {
            StudentProfile p = new StudentProfile(user);
            p.setGradeLevel("Grade 3");
            p.setCurrentReadingLevel(1);
            p.setFontPreference("OpenDyslexic");
            p.setThemePreference("theme-offwhite");
            return p;
        });

        List<ReadingSession> readingSessions = readingSessionService.getSessionsByStudent(user);

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("parentLinked", parentStudentRepository.existsByStudent(user));
        model.addAttribute("readingLevel", profile.getCurrentReadingLevel());
        model.addAttribute("readingMinutes", profile.getTotalReadingMinutes());
        model.addAttribute("wordsPracticed", profile.getWordsPracticedCount());
        model.addAttribute("accuracyRate", 92);
        model.addAttribute("activeStreakDays", calculateReadingStreak(readingSessions));
        model.addAttribute("quizzes", quizService.getAllQuizzes());

        return "student/dashboard";
    }

    private int calculateReadingStreak(List<ReadingSession> sessions) {
        Set<LocalDate> practiceDays = sessions.stream()
                .map(ReadingSession::getCreatedAt)
                .filter(createdAt -> createdAt != null)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());
        LocalDate day = LocalDate.now();
        if (!practiceDays.contains(day)) {
            day = day.minusDays(1);
            if (!practiceDays.contains(day)) return 0;
        }
        int streak = 0;
        while (practiceDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    @GetMapping("/reading")
    public String readingLibrary(@RequestParam(value = "level", required = false) Integer level,
                                 @RequestParam(value = "category", required = false) String category,
                                 @RequestParam(value = "q", required = false) String query,
                                 Principal principal,
                                 Model model) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        StudentProfile profile = user != null ? userService.getStudentProfile(user).orElse(null) : null;

        Integer selectedLevel = level != null && level >= 1 && level <= 5 ? level : null;
        String selectedCategory = category == null || category.isBlank() ? null : category.trim();
        String selectedQuery = query == null || query.isBlank() ? null : query.trim();
        List<ReadingMaterial> materials = readingMaterialService.filterMaterials(
                selectedLevel, selectedCategory, selectedQuery);

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("materials", materials);
        model.addAttribute("selectedLevel", selectedLevel);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("searchQuery", selectedQuery);
        model.addAttribute("categories", readingMaterialService.getCategories());

        return "student/reading-library";
    }

    @GetMapping("/reading/{id}")
    public String smartReader(@PathVariable("id") Long id, Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        StudentProfile profile = user != null ? userService.getStudentProfile(user).orElse(null) : null;

        ReadingMaterial material = readingMaterialService.getMaterialById(id)
                    .orElse(null);

        if (material == null) {
            return "redirect:/student/reading";
        }

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("material", material);

        return "student/reader";
    }
}
