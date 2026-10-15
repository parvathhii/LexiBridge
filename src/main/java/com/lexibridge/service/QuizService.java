package com.lexibridge.service;

import com.lexibridge.dto.QuizSubmissionDto;
import com.lexibridge.entity.Quiz;
import com.lexibridge.entity.QuizAttempt;
import com.lexibridge.entity.User;

import java.util.List;
import java.util.Optional;

public interface QuizService {
    List<Quiz> getAllQuizzes();
    Optional<Quiz> getQuizById(Long id);
    QuizAttempt gradeAndRecordAttempt(User student, QuizSubmissionDto submission);
    List<QuizAttempt> getAttemptsByStudent(User student);
}
