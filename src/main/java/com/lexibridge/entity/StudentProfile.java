package com.lexibridge.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "age")
    private Integer age;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel = "Grade 3";

    @Column(name = "current_reading_level", nullable = false)
    private Integer currentReadingLevel = 1;

    @Column(name = "font_preference", length = 50)
    private String fontPreference = "OpenDyslexic";

    @Column(name = "font_size")
    private Integer fontSize = 18;

    @Column(name = "line_spacing")
    private Double lineSpacing = 1.75;

    @Column(name = "theme_preference", length = 50)
    private String themePreference = "theme-offwhite";

    @Column(name = "reading_ruler_enabled")
    private Boolean readingRulerEnabled = false;

    @Column(name = "letter_spacing", length = 16)
    private String letterSpacing = "0.06em";

    @Column(name = "reading_text_color", length = 7)
    private String readingTextColor = "#26364a";

    @Column(name = "demo_progress_seeded")
    private Boolean demoProgressSeeded = false;

    @Column(name = "speech_rate")
    private Double speechRate = 0.85;

    @Column(name = "tts_voice", length = 100)
    private String ttsVoice = "default";

    @Column(name = "total_reading_minutes")
    private Integer totalReadingMinutes = 0;

    @Column(name = "words_practiced_count")
    private Integer wordsPracticedCount = 0;

    @JsonIgnore
    @Column(name = "parent_link_code", unique = true, length = 20)
    private String parentLinkCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public StudentProfile() {
    }

    public StudentProfile(User user) {
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public Integer getCurrentReadingLevel() {
        return currentReadingLevel;
    }

    public void setCurrentReadingLevel(Integer currentReadingLevel) {
        this.currentReadingLevel = currentReadingLevel;
    }

    public String getFontPreference() {
        return fontPreference;
    }

    public void setFontPreference(String fontPreference) {
        this.fontPreference = fontPreference;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public Double getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(Double lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference;
    }

    public Boolean getReadingRulerEnabled() {
        return readingRulerEnabled;
    }

    public void setReadingRulerEnabled(Boolean readingRulerEnabled) {
        this.readingRulerEnabled = readingRulerEnabled;
    }

    public String getLetterSpacing() { return letterSpacing; }

    public void setLetterSpacing(String letterSpacing) { this.letterSpacing = letterSpacing; }

    public String getReadingTextColor() { return readingTextColor; }

    public void setReadingTextColor(String readingTextColor) { this.readingTextColor = readingTextColor; }

    public Boolean getDemoProgressSeeded() { return demoProgressSeeded; }

    public void setDemoProgressSeeded(Boolean demoProgressSeeded) { this.demoProgressSeeded = demoProgressSeeded; }

    public Double getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(Double speechRate) {
        this.speechRate = speechRate;
    }

    public String getTtsVoice() {
        return ttsVoice;
    }

    public void setTtsVoice(String ttsVoice) {
        this.ttsVoice = ttsVoice;
    }

    public Integer getTotalReadingMinutes() {
        return totalReadingMinutes;
    }

    public void setTotalReadingMinutes(Integer totalReadingMinutes) {
        this.totalReadingMinutes = totalReadingMinutes;
    }

    public Integer getWordsPracticedCount() {
        return wordsPracticedCount;
    }

    public void setWordsPracticedCount(Integer wordsPracticedCount) {
        this.wordsPracticedCount = wordsPracticedCount;
    }

    public String getParentLinkCode() {
        return parentLinkCode;
    }

    public void setParentLinkCode(String parentLinkCode) {
        this.parentLinkCode = parentLinkCode;
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
