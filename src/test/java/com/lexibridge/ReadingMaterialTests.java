package com.lexibridge;

import com.lexibridge.dto.ReadingSessionDto;
import com.lexibridge.entity.ReadingMaterial;
import com.lexibridge.entity.ReadingSession;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.repository.ReadingMaterialRepository;
import com.lexibridge.repository.StudentProfileRepository;
import com.lexibridge.repository.UserRepository;
import com.lexibridge.service.ReadingMaterialService;
import com.lexibridge.service.ReadingSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReadingMaterialTests {

    @Autowired
    private ReadingMaterialRepository readingMaterialRepository;

    @Autowired
    private ReadingMaterialService readingMaterialService;

    @Autowired
    private ReadingSessionService readingSessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSeededReadingMaterialsExist() {
        List<ReadingMaterial> all = readingMaterialRepository.findAll();
        assertFalse(all.isEmpty(), "Seeded reading materials should exist");

        List<ReadingMaterial> level1 = readingMaterialRepository.findByReadingLevel(1);
        assertFalse(level1.isEmpty(), "Level 1 materials should exist");
    }

    @Test
    void testRecordReadingSessionUpdatesProfile() {
        User student = userRepository.findByUsername("student")
                .orElseThrow(() -> new IllegalStateException("Demo student must exist"));

        List<ReadingMaterial> materials = readingMaterialRepository.findAll();
        assertFalse(materials.isEmpty());
        ReadingMaterial first = materials.get(0);

        StudentProfile profileBefore = studentProfileRepository.findByUser(student).orElseThrow();
        int initialMinutes = profileBefore.getTotalReadingMinutes();
        int initialWords = profileBefore.getWordsPracticedCount();

        ReadingSessionDto dto = new ReadingSessionDto();
        dto.setMaterialId(first.getId());
        dto.setDurationSeconds(120);
        dto.setWordsRead(first.getWordCount());
        dto.setWordsPerMinute(46.0);
        dto.setTtsUsed(true);
        dto.setCompleted(true);

        ReadingSession recorded = readingSessionService.recordSession(student, dto);
        assertNotNull(recorded.getId());
        assertEquals(120, recorded.getDurationSeconds());
        assertTrue(recorded.getTtsUsed());

        StudentProfile profileAfter = studentProfileRepository.findByUser(student).orElseThrow();
        assertTrue(profileAfter.getTotalReadingMinutes() > initialMinutes, "Reading minutes should increment");
        assertTrue(profileAfter.getWordsPracticedCount() > initialWords, "Words practiced count should increment");
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void testStudentReadingLibraryAccessibleWithAuth() throws Exception {
        mockMvc.perform(get("/student/reading"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("materials"));
    }

    @Test
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void testStudentSmartReaderAccessibleWithAuth() throws Exception {
        List<ReadingMaterial> materials = readingMaterialRepository.findAll();
        assertFalse(materials.isEmpty());
        Long materialId = materials.get(0).getId();

        mockMvc.perform(get("/student/reading/" + materialId))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("material"));
    }
}
