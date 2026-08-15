package com.skillforge.service;

import com.skillforge.dto.ProgressResponseDTO;
import com.skillforge.repository.ModuleRepository;
import org.springframework.stereotype.Service;

@Service
public class ProgressService {

    private final ModuleRepository moduleRepository;
    private final AiRoadmapService aiRoadmapService;

    public ProgressService(ModuleRepository moduleRepository, AiRoadmapService aiRoadmapService) {
        this.moduleRepository = moduleRepository;
        this.aiRoadmapService = aiRoadmapService;
    }

    public ProgressResponseDTO getProgress(Long roadmapId) {
        aiRoadmapService.getOwnedRoadmap(roadmapId);
        long total = moduleRepository.countByRoadmapId(roadmapId);
        long completed = moduleRepository.countByRoadmapIdAndCompletedTrue(roadmapId);
        int percent = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);
        return new ProgressResponseDTO(total, completed, percent);
    }
}
