package com.lexibridge.service;

import com.lexibridge.dto.GameAttemptDto;
import com.lexibridge.entity.GameAttempt;
import com.lexibridge.entity.User;

import java.util.List;

public interface GameService {
    GameAttempt recordGameAttempt(User student, GameAttemptDto dto);
    List<GameAttempt> getAttemptsByStudent(User student);
    long countAttemptsByStudent(User student);
}
