package com.skillforge.dto;

public class ProgressResponseDTO {

    private long totalModules;
    private long completedModules;
    private int completionPercent;

    public ProgressResponseDTO() {
    }

    public ProgressResponseDTO(long totalModules, long completedModules, int completionPercent) {
        this.totalModules = totalModules;
        this.completedModules = completedModules;
        this.completionPercent = completionPercent;
    }

    public long getTotalModules() {
        return totalModules;
    }

    public long getCompletedModules() {
        return completedModules;
    }

    public int getCompletionPercent() {
        return completionPercent;
    }
}
