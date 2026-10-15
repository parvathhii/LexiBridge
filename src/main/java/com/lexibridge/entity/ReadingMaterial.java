package com.lexibridge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_materials", indexes = {
        @Index(name = "idx_reading_level", columnList = "reading_level"),
        @Index(name = "idx_reading_category", columnList = "category")
})
public class ReadingMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 255)
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 50)
    private String category = "Story";

    @NotNull(message = "Reading level is required")
    @Column(name = "reading_level", nullable = false)
    private Integer readingLevel = 1; // 1 to 5

    @Column(name = "target_phonemes", length = 100)
    private String targetPhonemes; // e.g. "b/d distinction", "vowel teams (ea/ee)"

    @Column(name = "estimated_reading_minutes")
    private Integer estimatedReadingMinutes = 3;

    @Column(name = "word_count")
    private Integer wordCount = 100;

    @Column(name = "cover_emoji", length = 10)
    private String coverEmoji = "📖";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ReadingMaterial() {
    }

    public ReadingMaterial(String title, String description, String content, String category,
                           Integer readingLevel, String targetPhonemes, Integer estimatedMinutes,
                           Integer wordCount, String coverEmoji, User createdBy) {
        this.title = title;
        this.description = description;
        this.content = content;
        this.category = category;
        this.readingLevel = readingLevel;
        this.targetPhonemes = targetPhonemes;
        this.estimatedReadingMinutes = estimatedMinutes;
        this.wordCount = wordCount;
        this.coverEmoji = coverEmoji;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getReadingLevel() {
        return readingLevel;
    }

    public void setReadingLevel(Integer readingLevel) {
        this.readingLevel = readingLevel;
    }

    public String getTargetPhonemes() {
        return targetPhonemes;
    }

    public void setTargetPhonemes(String targetPhonemes) {
        this.targetPhonemes = targetPhonemes;
    }

    public Integer getEstimatedReadingMinutes() {
        return estimatedReadingMinutes;
    }

    public void setEstimatedReadingMinutes(Integer estimatedReadingMinutes) {
        this.estimatedReadingMinutes = estimatedReadingMinutes;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public String getCoverEmoji() {
        return coverEmoji;
    }

    public void setCoverEmoji(String coverEmoji) {
        this.coverEmoji = coverEmoji;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
