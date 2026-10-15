package com.lexibridge.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "parent_homework_reminders", indexes = {
        @Index(name = "idx_parent_reminder_parent", columnList = "parent_id"),
        @Index(name = "idx_parent_reminder_student", columnList = "student_id")
})
public class ParentReminder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String note;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ParentReminder() { }

    public ParentReminder(User parent, User student, String title, String note, LocalDate dueDate) {
        this.parent = parent;
        this.student = student;
        this.title = title;
        this.note = note;
        this.dueDate = dueDate;
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public User getParent() { return parent; }
    public User getStudent() { return student; }
    public String getTitle() { return title; }
    public String getNote() { return note; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
