package com.lexibridge.service;

import com.lexibridge.repository.StudentProfileRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class StudentLinkCodeGenerator {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 12;

    private final SecureRandom random = new SecureRandom();
    private final StudentProfileRepository studentProfileRepository;

    public StudentLinkCodeGenerator(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    public String createUniqueCode() {
        String code;
        do {
            StringBuilder value = new StringBuilder("LB-");
            for (int i = 0; i < CODE_LENGTH; i++) {
                if (i == 6) value.append('-');
                value.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            code = value.toString();
        } while (studentProfileRepository.existsByParentLinkCodeIgnoreCase(code));
        return code;
    }
}
