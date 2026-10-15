package com.lexibridge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_sessions", indexes = {
        @Index(name = "idx_reading_session_student", columnList = "student_id"),
        @Index(name = "idx_reading_session_material", columnList = "material_id")
})
public class ReadingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private ReadingMaterial material;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds = 0;

    @Column(name = "words_read", nullable = false)
    private Integer wordsRead = 0;

    @Column(name = "words_per_minute")
    private Double wordsPerMinute = 0.0;

    @Column(name = "reading_accuracy")
    private Integer readingAccuracy;

    @Column(name = "skipped_word_count", nullable = false, columnDefinition = "integer not null default 0")
    private Integer skippedWordCount = 0;

    @Column(name = "repeated_word_count", nullable = false, columnDefinition = "integer not null default 0")
    private Integer repeatedWordCount = 0;

    @Column(name = "tts_used")
    private Boolean ttsUsed = false;

    @Column(name = "completed", nullable = false)
    private Boolean completed = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ReadingSession() {
    }

    public ReadingSession(User student, ReadingMaterial material, Integer durationSeconds,
                          Integer wordsRead, Double wordsPerMinute, Boolean ttsUsed, Boolean completed) {
        this.student = student;
        this.material = material;
        this.durationSeconds = durationSeconds;
        this.wordsRead = wordsRead;
        this.wordsPerMinute = wordsPerMinute;
        this.ttsUsed = ttsUsed;
        this.completed = completed;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
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

    public ReadingMaterial getMaterial() {
        return material;
    }

    public void setMaterial(ReadingMaterial material) {
        this.material = material;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getWordsRead() {
        return wordsRead;
    }

    public void setWordsRead(Integer wordsRead) {
        this.wordsRead = wordsRead;
    }

    public Double getWordsPerMinute() {
        return wordsPerMinute;
    }

    public void setWordsPerMinute(Double wordsPerMinute) {
        this.wordsPerMinute = wordsPerMinute;
    }

    public Integer getReadingAccuracy() { return readingAccuracy; }
    public void setReadingAccuracy(Integer readingAccuracy) { this.readingAccuracy = readingAccuracy; }
    public Integer getSkippedWordCount() { return skippedWordCount; }
    public void setSkippedWordCount(Integer skippedWordCount) { this.skippedWordCount = skippedWordCount; }
    public Integer getRepeatedWordCount() { return repeatedWordCount; }
    public void setRepeatedWordCount(Integer repeatedWordCount) { this.repeatedWordCount = repeatedWordCount; }

    public Boolean getTtsUsed() {
        return ttsUsed;
    }

    public void setTtsUsed(Boolean ttsUsed) {
        this.ttsUsed = ttsUsed;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
