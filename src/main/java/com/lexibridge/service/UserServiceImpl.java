package com.lexibridge.service;

import com.lexibridge.dto.UserRegistrationDto;
import com.lexibridge.entity.Role;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.repository.StudentProfileRepository;
import com.lexibridge.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentLinkCodeGenerator linkCodeGenerator;
    private final ParentStudentLinkService parentStudentLinkService;

    public UserServiceImpl(UserRepository userRepository,
                           StudentProfileRepository studentProfileRepository,
                           PasswordEncoder passwordEncoder,
                           StudentLinkCodeGenerator linkCodeGenerator,
                           ParentStudentLinkService parentStudentLinkService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.linkCodeGenerator = linkCodeGenerator;
        this.parentStudentLinkService = parentStudentLinkService;
    }

    @Override
    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (dto.getRole() != Role.ROLE_STUDENT && dto.getRole() != Role.ROLE_PARENT && dto.getRole() != Role.ROLE_TEACHER) {
            throw new IllegalArgumentException("Choose Student, Parent, or Teacher to create an account.");
        }

        if (dto.getRole() == Role.ROLE_PARENT) {
            if (dto.getStudentLinkCode() == null || dto.getStudentLinkCode().isBlank()) {
                throw new IllegalArgumentException("Enter the student registration code to link your parent account.");
            }
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered");
        }

        User user = new User();
        user.setFullName(dto.getFullName().trim());
        user.setUsername(dto.getUsername().trim());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        if (dto.getRole() == Role.ROLE_STUDENT) {
            StudentProfile profile = new StudentProfile(savedUser);
            profile.setGradeLevel(dto.getGradeLevel() != null ? dto.getGradeLevel() : "Grade 3");
            profile.setAge(dto.getAge() != null ? dto.getAge() : 8);
            profile.setCurrentReadingLevel(1);
            profile.setFontPreference("OpenDyslexic");
            profile.setFontSize(18);
            profile.setLineSpacing(1.75);
            profile.setThemePreference("theme-offwhite");
            profile.setParentLinkCode(linkCodeGenerator.createUniqueCode());
            studentProfileRepository.save(profile);
        }

        if (dto.getRole() == Role.ROLE_PARENT) {
            parentStudentLinkService.linkUsingCode(savedUser, dto.getStudentLinkCode());
        }

        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudentProfile> getStudentProfile(User user) {
        return studentProfileRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.findByRole(role).size();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAllUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional
    public void setAccountEnabled(Long userId, boolean enabled, String adminUsername) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("That user account could not be found."));
        if (!enabled && target.getUsername().equals(adminUsername)) {
            throw new IllegalArgumentException("You cannot disable the account you are currently using.");
        }
        if (!enabled && target.getRole() == Role.ROLE_ADMIN && target.isEnabled()
                && userRepository.countByRoleAndEnabledTrue(Role.ROLE_ADMIN) <= 1) {
            throw new IllegalArgumentException("At least one active administrator account must remain enabled.");
        }
        target.setEnabled(enabled);
        userRepository.save(target);
    }
}
