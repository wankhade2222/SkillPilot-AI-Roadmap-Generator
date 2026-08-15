package com.skillforge.dto;

public class ModuleDTO {

    private Long id;
    private Long roadmapId;
    private String title;
    private String description;
    private String freeResource;
    private String paidResource;
    private int moduleOrder;
    private boolean completed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoadmapId() {
        return roadmapId;
    }

    public void setRoadmapId(Long roadmapId) {
        this.roadmapId = roadmapId;
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

    public String getFreeResource() {
        return freeResource;
    }

    public void setFreeResource(String freeResource) {
        this.freeResource = freeResource;
    }

    public String getPaidResource() {
        return paidResource;
    }

    public void setPaidResource(String paidResource) {
        this.paidResource = paidResource;
    }

    public int getModuleOrder() {
        return moduleOrder;
    }

    public void setModuleOrder(int moduleOrder) {
        this.moduleOrder = moduleOrder;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
