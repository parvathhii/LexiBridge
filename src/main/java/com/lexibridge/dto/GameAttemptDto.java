package com.lexibridge.dto;

public class GameAttemptDto {
    private String gameCode;
    private Integer score;
    private Double accuracy;
    private String confusedLetters;
    private Integer timeSpentSeconds;
    private Integer difficultyLevel;

    public GameAttemptDto() {
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

    public Integer getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; }
}
