package com.skillforge.controller;

import com.skillforge.dto.GenerateRoadmapRequest;
import com.skillforge.dto.RoadmapResponseDTO;
import com.skillforge.dto.RoadmapSummaryDTO;
import com.skillforge.service.AiRoadmapService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiRoadmapService aiRoadmapService;

    public AiController(AiRoadmapService aiRoadmapService) {
        this.aiRoadmapService = aiRoadmapService;
    }

    @PostMapping("/generate-roadmap")
    public RoadmapResponseDTO generateRoadmap(@Valid @RequestBody GenerateRoadmapRequest request) {
        return aiRoadmapService.generateRoadmap(request);
    }

    @GetMapping("/roadmaps")
    public List<RoadmapSummaryDTO> getRoadmaps() {
        return aiRoadmapService.getRoadmapsForCurrentUser();
    }
}
