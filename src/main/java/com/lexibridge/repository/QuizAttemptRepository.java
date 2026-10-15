package com.lexibridge.repository;

import com.lexibridge.entity.Quiz;
import com.lexibridge.entity.QuizAttempt;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    @EntityGraph(attributePaths = "quiz")
    List<QuizAttempt> findByStudent(User student);
    List<QuizAttempt> findByStudentAndQuiz(User student, Quiz quiz);
    long countByStudent(User student);
    @EntityGraph(attributePaths = "quiz")
    List<QuizAttempt> findTop10ByStudentOrderByCompletedAtDesc(User student);
}
