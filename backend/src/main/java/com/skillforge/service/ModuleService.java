package com.skillforge.service;

import com.skillforge.dto.ModuleDTO;
import com.skillforge.entity.Module;
import com.skillforge.exception.ForbiddenException;
import com.skillforge.exception.ResourceNotFoundException;
import com.skillforge.repository.ModuleRepository;
import com.skillforge.util.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final AiRoadmapService aiRoadmapService;

    public ModuleService(ModuleRepository moduleRepository, AiRoadmapService aiRoadmapService) {
        this.moduleRepository = moduleRepository;
        this.aiRoadmapService = aiRoadmapService;
    }

    public List<ModuleDTO> getModulesForRoadmap(Long roadmapId) {
        aiRoadmapService.getOwnedRoadmap(roadmapId);
        return moduleRepository.findByRoadmapIdOrderByModuleOrderAsc(roadmapId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ModuleDTO markComplete(Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!module.getRoadmap().getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this module");
        }
        module.setCompleted(true);
        Module savedModule = moduleRepository.save(module);
        long totalModules = moduleRepository.countByRoadmapId(savedModule.getRoadmap().getId());
        long completedModules = moduleRepository.countByRoadmapIdAndCompletedTrue(savedModule.getRoadmap().getId());
        savedModule.getRoadmap().setCompleted(totalModules > 0 && totalModules == completedModules);
        return toDto(savedModule);
    }

    private ModuleDTO toDto(Module module) {
        ModuleDTO dto = new ModuleDTO();
        dto.setId(module.getId());
        dto.setRoadmapId(module.getRoadmap().getId());
        dto.setTitle(module.getTitle());
        dto.setDescription(module.getDescription());
        dto.setFreeResource(module.getFreeResource());
        dto.setPaidResource(module.getPaidResource());
        dto.setModuleOrder(module.getModuleOrder());
        dto.setCompleted(module.isCompleted());
        return dto;
    }
}
