package com.lexibridge.controller;

import com.lexibridge.entity.ParentTeacherFeedback;
import com.lexibridge.entity.Role;
import com.lexibridge.entity.User;
import com.lexibridge.service.ParentTeacherFeedbackService;
import com.lexibridge.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.List;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {
    private static final List<String> CATEGORIES = List.of(
            "Learning progress", "Reading support", "Speech practice", "Classroom update", "Other");

    private final ParentTeacherFeedbackService feedbackService;
    private final UserService userService;

    public FeedbackController(ParentTeacherFeedbackService feedbackService, UserService userService) {
        this.feedbackService = feedbackService;
        this.userService = userService;
    }

    @GetMapping
    public String page(Principal principal, Model model) {
        User user = currentUser(principal);
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.ROLE_PARENT && user.getRole() != Role.ROLE_TEACHER) return "redirect:/";

        List<ParentTeacherFeedback> history = feedbackService.historyAndMarkRead(user);
        model.addAttribute("user", user);
        model.addAttribute("students", feedbackService.linkedStudents(user));
        model.addAttribute("history", history);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("senderRole", user.getRole() == Role.ROLE_PARENT ? "Parent" : "Teacher");
        return "feedback/index";
    }

    @GetMapping(value = "/unread-count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Long> unreadCount(Principal principal) {
        User user = currentUser(principal);
        if (user == null || (user.getRole() != Role.ROLE_PARENT && user.getRole() != Role.ROLE_TEACHER)) {
            return Map.of("count", 0L);
        }
        return Map.of("count", feedbackService.unreadCount(user));
    }

    @PostMapping
    public String submit(@RequestParam Long studentId,
                         @RequestParam String category,
                         @RequestParam String feedbackMessage,
                         Principal principal,
                         RedirectAttributes redirect) {
        User sender = currentUser(principal);
        if (sender == null) return "redirect:/login";
        try {
            if (!CATEGORIES.contains(category)) throw new IllegalArgumentException("Choose a feedback category.");
            String cleanText = feedbackMessage == null ? "" : feedbackMessage.trim();
            if (cleanText.isBlank() || cleanText.length() > 2000) {
                throw new IllegalArgumentException("Write a message of 1 to 2,000 characters.");
            }
            User student = userService.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Choose a valid student."));
            int recipientCount = feedbackService.submit(sender, student, category, cleanText);
            redirect.addFlashAttribute("successMessage", "Your feedback was sent to " + recipientCount +
                    (recipientCount == 1 ? " linked contact." : " linked contacts."));
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/feedback";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String reply,
                        Principal principal, RedirectAttributes redirect) {
        User user = currentUser(principal);
        if (user == null) return "redirect:/login";
        try {
            String cleanReply = reply == null ? "" : reply.trim();
            if (cleanReply.isBlank() || cleanReply.length() > 2000) {
                throw new IllegalArgumentException("Write a reply of 1 to 2,000 characters.");
            }
            feedbackService.reply(user, id, cleanReply);
            redirect.addFlashAttribute("successMessage", "Your reply has been added.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/feedback";
    }

    private User currentUser(Principal principal) {
        return principal == null ? null : userService.findByUsername(principal.getName()).orElse(null);
    }
}
