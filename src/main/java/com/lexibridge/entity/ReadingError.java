package com.lexibridge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_errors", indexes = {
        @Index(name = "idx_reading_error_student", columnList = "student_id"),
        @Index(name = "idx_reading_error_type", columnList = "error_type")
})
public class ReadingError {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ReadingSession session;

    @Column(name = "error_type", nullable = false, length = 32)
    private String errorType;

    @Column(nullable = false, length = 100)
    private String pattern;

    @Column(nullable = false)
    private Integer occurrences = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ReadingError() { }

    public ReadingError(User student, ReadingSession session, String errorType, String pattern, Integer occurrences) {
        this.student = student;
        this.session = session;
        this.errorType = errorType;
        this.pattern = pattern;
        this.occurrences = occurrences;
    }

    @PrePersist
    protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public User getStudent() { return student; }
    public ReadingSession getSession() { return session; }
    public String getErrorType() { return errorType; }
    public String getPattern() { return pattern; }
    public Integer getOccurrences() { return occurrences; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
