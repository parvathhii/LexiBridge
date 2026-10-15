package com.lexibridge.service;

import com.lexibridge.dto.GameAttemptDto;
import com.lexibridge.entity.GameAttempt;
import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import com.lexibridge.repository.GameAttemptRepository;
import com.lexibridge.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameServiceImpl implements GameService {

    private final GameAttemptRepository gameAttemptRepository;
    private final StudentProfileRepository studentProfileRepository;

    public GameServiceImpl(GameAttemptRepository gameAttemptRepository,
                           StudentProfileRepository studentProfileRepository) {
        this.gameAttemptRepository = gameAttemptRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Override
    @Transactional
    public GameAttempt recordGameAttempt(User student, GameAttemptDto dto) {
        GameAttempt attempt = new GameAttempt();
        attempt.setStudent(student);
        attempt.setGameCode(dto.getGameCode() != null ? dto.getGameCode() : "GENERAL");
        attempt.setScore(dto.getScore() != null ? dto.getScore() : 0);
        attempt.setAccuracy(dto.getAccuracy() != null ? dto.getAccuracy() : 100.0);
        attempt.setDifficultyLevel(dto.getDifficultyLevel() != null && dto.getDifficultyLevel() >= 1 && dto.getDifficultyLevel() <= 3 ? dto.getDifficultyLevel() : 1);
        attempt.setConfusedLetters(dto.getConfusedLetters());
        attempt.setTimeSpentSeconds(dto.getTimeSpentSeconds() != null ? dto.getTimeSpentSeconds() : 60);

        GameAttempt saved = gameAttemptRepository.save(attempt);

        // Update student profile practice words/minutes
        studentProfileRepository.findByUser(student).ifPresent(p -> {
            p.setWordsPracticedCount(p.getWordsPracticedCount() + 5);
            studentProfileRepository.save(p);
        });

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameAttempt> getAttemptsByStudent(User student) {
        return gameAttemptRepository.findByStudentOrderByCompletedAtDesc(student);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAttemptsByStudent(User student) {
        return gameAttemptRepository.countByStudent(student);
    }
}
