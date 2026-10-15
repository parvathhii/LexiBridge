package com.lexibridge.dto;

public class SpeechAttemptRequest {
    private String targetWord;
    private String recognizedText;
    private Integer level;
    public String getTargetWord() { return targetWord; }
    public void setTargetWord(String targetWord) { this.targetWord = targetWord; }
    public String getRecognizedText() { return recognizedText; }
    public void setRecognizedText(String recognizedText) { this.recognizedText = recognizedText; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
}
