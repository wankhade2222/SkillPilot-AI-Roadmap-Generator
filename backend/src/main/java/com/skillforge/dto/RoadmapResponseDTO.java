package com.skillforge.dto;

import java.util.ArrayList;
import java.util.List;

public class RoadmapResponseDTO {

    private Long roadmapId;
    private String goal;
    private String level;
    private String preferences;
    private String generationMode;
    private String generationMessage;
    private List<ModuleDTO> modules = new ArrayList<>();

    public Long getRoadmapId() {
        return roadmapId;
    }

    public void setRoadmapId(Long roadmapId) {
        this.roadmapId = roadmapId;
    }

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

    public String getGenerationMode() {
        return generationMode;
    }

    public void setGenerationMode(String generationMode) {
        this.generationMode = generationMode;
    }

    public String getGenerationMessage() {
        return generationMessage;
    }

    public void setGenerationMessage(String generationMessage) {
        this.generationMessage = generationMessage;
    }

    public List<ModuleDTO> getModules() {
        return modules;
    }

    public void setModules(List<ModuleDTO> modules) {
        this.modules = modules;
    }
}
