package com.lexibridge.controller;

import com.lexibridge.dto.QuizSubmissionDto;
import com.lexibridge.entity.Quiz;
import com.lexibridge.entity.QuizAttempt;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.service.QuizService;
import com.lexibridge.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/student/quiz")
public class QuizController {

    private final QuizService quizService;
    private final UserService userService;

    public QuizController(QuizService quizService, UserService userService) {
        this.quizService = quizService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public String viewQuiz(@PathVariable("id") Long id, Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        StudentProfile profile = user != null ? userService.getStudentProfile(user).orElse(null) : null;

        Quiz quiz = quizService.getQuizById(id).orElse(null);
        if (quiz == null) {
            return "redirect:/student/reading";
        }

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("quiz", quiz);

        return "student/quiz/quiz-view";
    }

    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitQuiz(@RequestBody QuizSubmissionDto submission, Principal principal) {
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
            QuizAttempt attempt = quizService.gradeAndRecordAttempt(user, submission);
            response.put("success", true);
            response.put("score", attempt.getScore());
            response.put("totalQuestions", attempt.getTotalQuestions());
            double percentage = attempt.getTotalQuestions() > 0
                    ? (attempt.getScore() * 100.0) / attempt.getTotalQuestions()
                    : 0.0;
            response.put("percentage", percentage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
