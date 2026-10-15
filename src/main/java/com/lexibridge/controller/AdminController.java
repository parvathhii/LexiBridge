package com.lexibridge.controller;

import com.lexibridge.entity.Role;
import com.lexibridge.entity.User;
import com.lexibridge.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User admin = userService.findByUsername(principal.getName()).orElse(null);
        List<User> allUsers = userService.getAllUsers();

        model.addAttribute("admin", admin);
        model.addAttribute("users", allUsers);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("studentCount", userService.countByRole(Role.ROLE_STUDENT));
        model.addAttribute("parentCount", userService.countByRole(Role.ROLE_PARENT));
        model.addAttribute("teacherCount", userService.countByRole(Role.ROLE_TEACHER));
        model.addAttribute("adminCount", userService.countByRole(Role.ROLE_ADMIN));

        return "admin/dashboard";
    }

    @PostMapping("/users/{id}/status")
    public String updateAccountStatus(@PathVariable Long id,
                                      @RequestParam boolean enabled,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            userService.setAccountEnabled(id, enabled, principal.getName());
            redirectAttributes.addFlashAttribute("adminNotice", enabled ? "Account enabled." : "Account disabled.");
            redirectAttributes.addFlashAttribute("noticeType", "success");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("adminNotice", exception.getMessage());
            redirectAttributes.addFlashAttribute("noticeType", "error");
        }
        return "redirect:/admin/dashboard";
    }
}
