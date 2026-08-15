package com.skillforge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "module")
public class Module extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private AiRoadmap roadmap;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "free_resource", nullable = false, columnDefinition = "TEXT")
    private String freeResource;

    @Column(name = "paid_resource", nullable = false, columnDefinition = "TEXT")
    private String paidResource;

    @Column(name = "module_order", nullable = false)
    private int moduleOrder;

    @Column(nullable = false)
    private boolean completed;

    public Long getId() {
        return id;
    }

    public AiRoadmap getRoadmap() {
        return roadmap;
    }

    public void setRoadmap(AiRoadmap roadmap) {
        this.roadmap = roadmap;
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
