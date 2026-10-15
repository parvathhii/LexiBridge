package com.lexibridge.service;

import com.lexibridge.dto.UserRegistrationDto;
import com.lexibridge.entity.Role;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User registerUser(UserRegistrationDto dto);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    List<User> getAllUsers();
    List<User> getUsersByRole(Role role);
    Optional<StudentProfile> getStudentProfile(User user);
    long countByRole(Role role);
    long countAllUsers();
    void setAccountEnabled(Long userId, boolean enabled, String adminUsername);
}
