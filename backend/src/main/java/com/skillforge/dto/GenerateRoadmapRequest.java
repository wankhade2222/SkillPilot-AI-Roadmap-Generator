package com.skillforge.dto;

import jakarta.validation.constraints.NotBlank;

public class GenerateRoadmapRequest {

    @NotBlank
    private String goal;

    @NotBlank
    private String level;

    @NotBlank
    private String preferences;

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }
}
