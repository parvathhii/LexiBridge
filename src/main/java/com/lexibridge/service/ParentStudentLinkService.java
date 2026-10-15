package com.lexibridge.service;

import com.lexibridge.entity.ParentStudent;
import com.lexibridge.entity.Role;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.repository.ParentStudentRepository;
import com.lexibridge.repository.StudentProfileRepository;
import com.lexibridge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ParentStudentLinkService {
    private final ParentStudentRepository parentStudentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    public ParentStudentLinkService(ParentStudentRepository parentStudentRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    UserRepository userRepository) {
        this.parentStudentRepository = parentStudentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void linkUsingCode(User parent, String suppliedCode) {
        if (parent == null || parent.getRole() != Role.ROLE_PARENT) {
            throw new IllegalArgumentException("Only a parent account can be linked to a student.");
        }
        String code = suppliedCode == null ? "" : suppliedCode.trim().toUpperCase(Locale.ROOT);
        if (code.isBlank()) {
            throw new IllegalArgumentException("Enter the student registration code.");
        }

        User lockedParent = userRepository.findByIdForUpdate(parent.getId())
                .orElseThrow(() -> new IllegalArgumentException("Parent account could not be verified."));
        if (parentStudentRepository.existsByParent(lockedParent)) {
            throw new IllegalArgumentException("This parent account is already linked to a student.");
        }

        StudentProfile profile = studentProfileRepository.findByParentLinkCodeForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("That student code is invalid or has already been used. Ask the student for their current code."));
        User student = profile.getUser();
        if (student.getRole() != Role.ROLE_STUDENT || !student.isEnabled()) {
            throw new IllegalArgumentException("That code cannot be used to link this account. Ask the student for their current code.");
        }
        if (parentStudentRepository.existsByStudent(student)) {
            throw new IllegalArgumentException("This student is already linked to a parent account. Contact the school if the link needs to change.");
        }

        parentStudentRepository.save(new ParentStudent(lockedParent, student));
        profile.setParentLinkCode(null);
        studentProfileRepository.save(profile);
    }
}
