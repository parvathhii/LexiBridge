package com.lexibridge.repository;

import com.lexibridge.entity.SpeechAttempt;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpeechAttemptRepository extends JpaRepository<SpeechAttempt, Long> {
    List<SpeechAttempt> findTop8ByStudentOrderByCompletedAtDesc(User student);
    long countByStudent(User student);
    long countByStudentAndAccuracyGreaterThanEqual(User student, Integer accuracy);
    List<SpeechAttempt> findByStudentOrderByCompletedAtDesc(User student);
    List<SpeechAttempt> findTop10ByStudentOrderByCompletedAtDesc(User student);
}
