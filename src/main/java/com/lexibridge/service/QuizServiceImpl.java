package com.lexibridge.service;

import com.lexibridge.dto.QuizSubmissionDto;
import com.lexibridge.entity.Quiz;
import com.lexibridge.entity.QuizAttempt;
import com.lexibridge.entity.QuizQuestion;
import com.lexibridge.entity.User;
import com.lexibridge.repository.QuizAttemptRepository;
import com.lexibridge.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public QuizServiceImpl(QuizRepository quizRepository, QuizAttemptRepository quizAttemptRepository) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Quiz> getQuizById(Long id) {
        return quizRepository.findByIdWithQuestions(id);
    }

    @Override
    @Transactional
    public QuizAttempt gradeAndRecordAttempt(User student, QuizSubmissionDto submission) {
        Quiz quiz = quizRepository.findById(submission.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with ID: " + submission.getQuizId()));

        int total = quiz.getQuestions().size();
        int correct = 0;
        Map<Long, String> answers = submission.getAnswers();

        if (answers != null) {
            for (QuizQuestion q : quiz.getQuestions()) {
                String chosen = answers.get(q.getId());
                if (chosen != null && chosen.trim().equalsIgnoreCase(q.getCorrectOption())) {
                    correct++;
                }
            }
        }

        double pct = total > 0 ? (correct * 100.0) / total : 0.0;

        QuizAttempt attempt = new QuizAttempt(student, quiz, correct, total, Math.round(pct * 10.0) / 10.0);
        return quizAttemptRepository.save(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttempt> getAttemptsByStudent(User student) {
        return quizAttemptRepository.findByStudent(student);
    }
}
