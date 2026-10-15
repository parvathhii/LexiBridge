package com.lexibridge.repository;

import com.lexibridge.entity.GameAttempt;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameAttemptRepository extends JpaRepository<GameAttempt, Long> {
    List<GameAttempt> findByStudent(User student);
    List<GameAttempt> findByStudentOrderByCompletedAtDesc(User student);
    List<GameAttempt> findByStudentAndGameCode(User student, String gameCode);
    long countByStudent(User student);
    List<GameAttempt> findTop10ByStudentOrderByCompletedAtDesc(User student);
}
