package com.lexibridge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_attempts", indexes = {
        @Index(name = "idx_game_student", columnList = "student_id"),
        @Index(name = "idx_game_code", columnList = "game_code")
})
public class GameAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "game_code", nullable = false, length = 50)
    private String gameCode;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(nullable = false)
    private Double accuracy = 100.0;

    @Column(name = "difficulty_level", nullable = false, columnDefinition = "integer not null default 1")
    private Integer difficultyLevel = 1;

    @Column(name = "confused_letters", length = 100)
    private String confusedLetters; // e.g. "b-d, p-q" for heatmap tracking

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 60;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private LocalDateTime completedAt;

    public GameAttempt() {
    }

    public GameAttempt(User student, String gameCode, Integer score, Double accuracy,
                       String confusedLetters, Integer timeSpentSeconds) {
        this.student = student;
        this.gameCode = gameCode;
        this.score = score;
        this.accuracy = accuracy;
        this.confusedLetters = confusedLetters;
        this.timeSpentSeconds = timeSpentSeconds;
    }

    @PrePersist
    protected void onCreate() {
        if (this.completedAt == null) this.completedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public String getGameCode() {
        return gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Integer getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getConfusedLetters() {
        return confusedLetters;
    }

    public void setConfusedLetters(String confusedLetters) {
        this.confusedLetters = confusedLetters;
    }

    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
