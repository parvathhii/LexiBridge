package com.lexibridge.controller;

import com.lexibridge.entity.Role;
import com.lexibridge.entity.User;
import com.lexibridge.service.ParentTeacherFeedbackService;
import com.lexibridge.service.UserService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice(assignableTypes = {HomeController.class, ParentController.class, TeacherController.class, FeedbackController.class, WeeklyProgressController.class})
public class FeedbackNotificationAdvice {
    private final UserService userService;
    private final ParentTeacherFeedbackService feedbackService;

    public FeedbackNotificationAdvice(UserService userService, ParentTeacherFeedbackService feedbackService) {
        this.userService = userService;
        this.feedbackService = feedbackService;
    }

    @ModelAttribute
    public void addUnreadFeedbackCount(Principal principal, Model model) {
        long count = 0;
        if (principal != null) {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            if (user != null && (user.getRole() == Role.ROLE_PARENT || user.getRole() == Role.ROLE_TEACHER)) {
                count = feedbackService.unreadCount(user);
            }
        }
        model.addAttribute("feedbackUnreadCount", count);
    }
}
