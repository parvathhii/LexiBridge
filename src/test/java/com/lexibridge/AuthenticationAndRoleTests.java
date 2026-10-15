package com.lexibridge;

import com.lexibridge.dto.UserRegistrationDto;
import com.lexibridge.entity.Role;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.repository.UserRepository;
import com.lexibridge.service.CustomUserDetailsService;
import com.lexibridge.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationAndRoleTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testStudentRegistrationCreatesUserAndProfile() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setFullName("Kiran Rao");
        dto.setUsername("kiranrao_" + System.currentTimeMillis());
        dto.setEmail("kiran_" + System.currentTimeMillis() + "@test.edu");
        dto.setPassword("SecretPass@123");
        dto.setConfirmPassword("SecretPass@123");
        dto.setRole(Role.ROLE_STUDENT);
        dto.setGradeLevel("Grade 4");
        dto.setAge(9);

        User savedUser = userService.registerUser(dto);
        assertNotNull(savedUser.getId(), "User ID must be generated");
        assertEquals(Role.ROLE_STUDENT, savedUser.getRole());
        assertTrue(passwordEncoder.matches("SecretPass@123", savedUser.getPassword()), "BCrypt password should match");

        Optional<StudentProfile> profileOpt = userService.getStudentProfile(savedUser);
        assertTrue(profileOpt.isPresent(), "StudentProfile must be automatically created for ROLE_STUDENT");
        assertEquals("Grade 4", profileOpt.get().getGradeLevel());
        assertEquals(9, profileOpt.get().getAge());
    }

    @Test
    void testUserDetailsServiceLoadsByUsernameOrEmail() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("student");
        assertNotNull(userDetails, "Should load demo student");
        assertEquals("student", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")));

        UserDetails userDetailsEmail = userDetailsService.loadUserByUsername("student@lexibridge.edu");
        assertNotNull(userDetailsEmail, "Should load demo student by email");
        assertEquals("student", userDetailsEmail.getUsername());
    }

    @Test
    void testUnauthenticatedAccessToStudentDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void testUnauthenticatedAccessToAdminDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void testPublicEndpointsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk());
    }
}
