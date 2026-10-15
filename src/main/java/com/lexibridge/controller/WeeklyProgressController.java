package com.lexibridge.controller;

import com.lexibridge.entity.*;
import com.lexibridge.repository.ParentStudentRepository;
import com.lexibridge.repository.TeacherStudentRepository;
import com.lexibridge.service.UserService;
import com.lexibridge.service.WeeklyProgressService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Controller
public class WeeklyProgressController {
    private final UserService userService;
    private final ParentStudentRepository parentStudentRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final WeeklyProgressService weeklyProgressService;

    public WeeklyProgressController(UserService userService,
                                    ParentStudentRepository parentStudentRepository,
                                    TeacherStudentRepository teacherStudentRepository,
                                    WeeklyProgressService weeklyProgressService) {
        this.userService = userService;
        this.parentStudentRepository = parentStudentRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.weeklyProgressService = weeklyProgressService;
    }

    @GetMapping("/progress")
    public String weeklyProgress(@RequestParam(value = "studentId", required = false) Long studentId,
                                 Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        User viewer = userService.findByUsername(principal.getName()).orElse(null);
        if (viewer == null) return "redirect:/login";

        List<User> students;
        String viewerRole;
        if (viewer.getRole() == Role.ROLE_PARENT) {
            students = parentStudentRepository.findByParent(viewer).stream().map(ParentStudent::getStudent).toList();
            viewerRole = "Parent";
        } else if (viewer.getRole() == Role.ROLE_TEACHER) {
            students = teacherStudentRepository.findByTeacher(viewer).stream().map(TeacherStudent::getStudent).toList();
            viewerRole = "Teacher";
        } else {
            return "redirect:/";
        }

        User selectedStudent = students.stream()
                .filter(student -> studentId != null && Objects.equals(student.getId(), studentId))
                .findFirst().orElse(students.isEmpty() ? null : students.get(0));
        model.addAttribute("viewer", viewer);
        model.addAttribute("viewerRole", viewerRole);
        model.addAttribute("students", students);
        model.addAttribute("selectedStudent", selectedStudent);
        model.addAttribute("report", selectedStudent == null ? null : weeklyProgressService.buildReport(selectedStudent));
        return "progress/weekly";
    }
}
