package com.lexibridge.service;

import com.lexibridge.dto.ReadingSessionDto;
import com.lexibridge.entity.ReadingSession;
import com.lexibridge.entity.User;

import java.util.List;

public interface ReadingSessionService {
    ReadingSession recordSession(User student, ReadingSessionDto dto);
    List<ReadingSession> getSessionsByStudent(User student);
    long countSessionsByStudent(User student);
}
