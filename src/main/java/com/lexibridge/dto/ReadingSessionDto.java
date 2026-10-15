package com.lexibridge.dto;

public class ReadingSessionDto {
    private Long materialId;
    private Integer durationSeconds;
    private Integer wordsRead;
    private Double wordsPerMinute;
    private Boolean ttsUsed;
    private Boolean completed;
    private String spokenText;

    public ReadingSessionDto() {
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
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

    public String getSpokenText() { return spokenText; }
    public void setSpokenText(String spokenText) { this.spokenText = spokenText; }
}
