package com.lexibridge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "speech_attempts", indexes = @Index(name = "idx_speech_student", columnList = "student_id"))
public class SpeechAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 80)
    private String targetWord;

    @Column(nullable = false, length = 160)
    private String recognizedText;

    @Column(nullable = false)
    private Integer accuracy;

    @Column(nullable = false, columnDefinition = "integer not null default 1")
    private Integer level;

    @Column(nullable = false, updatable = false)
    private LocalDateTime completedAt;

    protected SpeechAttempt() { }

    public SpeechAttempt(User student, String targetWord, String recognizedText, Integer accuracy, Integer level) {
        this.student = student;
        this.targetWord = targetWord;
        this.recognizedText = recognizedText;
        this.accuracy = accuracy;
        this.level = level;
    }

    @PrePersist
    protected void onCreate() { if (completedAt == null) completedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public User getStudent() { return student; }
    public String getTargetWord() { return targetWord; }
    public String getRecognizedText() { return recognizedText; }
    public Integer getAccuracy() { return accuracy; }
    public Integer getLevel() { return level; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
