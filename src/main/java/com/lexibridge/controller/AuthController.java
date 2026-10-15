package com.lexibridge.controller;

import com.lexibridge.dto.UserRegistrationDto;
import com.lexibridge.entity.Role;
import com.lexibridge.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Account registered successfully! Please log in.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setRole(Role.ROLE_STUDENT);
        model.addAttribute("userDto", dto);
        model.addAttribute("roles", Role.values());
        return "register";
    }

    @PostMapping("/register/save")
    public String registerUser(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "register";
        }

        try {
            userService.registerUser(userDto);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            if (userDto.getRole() == Role.ROLE_PARENT && userDto.getStudentLinkCode() != null
                    && (ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("student")
                    || ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("code"))) {
                result.rejectValue("studentLinkCode", "invalid", ex.getMessage());
            }
            model.addAttribute("roles", Role.values());
            return "register";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Registration could not be completed. Please check the details and try again.");
            model.addAttribute("roles", Role.values());
            return "register";
        }
    }
}
