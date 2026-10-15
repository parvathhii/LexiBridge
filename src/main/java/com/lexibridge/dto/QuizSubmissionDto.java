package com.lexibridge.dto;

import java.util.Map;

public class QuizSubmissionDto {
    private Long quizId;
    private Map<Long, String> answers; // questionId -> selectedOption ("A", "B", "C", "D")

    public QuizSubmissionDto() {
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public Map<Long, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, String> answers) {
        this.answers = answers;
    }
}
