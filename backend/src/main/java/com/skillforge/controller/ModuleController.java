package com.skillforge.controller;

import com.skillforge.dto.ModuleDTO;
import com.skillforge.service.ModuleService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/modules")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping("/{roadmapId}")
    public List<ModuleDTO> getModules(@PathVariable Long roadmapId) {
        return moduleService.getModulesForRoadmap(roadmapId);
    }

    @PutMapping("/{id}/complete")
    public ModuleDTO markComplete(@PathVariable Long id) {
        return moduleService.markComplete(id);
    }
}
