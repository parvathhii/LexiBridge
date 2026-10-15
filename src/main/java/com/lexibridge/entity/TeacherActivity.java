package com.lexibridge.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_activities", indexes = {
        @Index(name = "idx_teacher_activity_student", columnList = "student_id"),
        @Index(name = "idx_teacher_activity_teacher", columnList = "teacher_id")
})
public class TeacherActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private User student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "material_id")
    private ReadingMaterial material;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "quiz_id")
    private Quiz quiz;
    @Column(nullable = false, length = 24)
    private String activityType;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(length = 500)
    private String instructions;
    private LocalDate dueDate;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TeacherActivity() { }
    public TeacherActivity(User teacher, User student, ReadingMaterial material, Quiz quiz,
                           String activityType, String title, String instructions, LocalDate dueDate) {
        this.teacher = teacher; this.student = student; this.material = material; this.quiz = quiz;
        this.activityType = activityType; this.title = title; this.instructions = instructions; this.dueDate = dueDate;
    }
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public User getTeacher() { return teacher; }
    public User getStudent() { return student; }
    public ReadingMaterial getMaterial() { return material; }
    public Quiz getQuiz() { return quiz; }
    public String getActivityType() { return activityType; }
    public String getTitle() { return title; }
    public String getInstructions() { return instructions; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
