package com.skillforge.controller;

import com.skillforge.dto.ProgressResponseDTO;
import com.skillforge.service.ProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/{roadmapId}")
    public ProgressResponseDTO getProgress(@PathVariable Long roadmapId) {
        return progressService.getProgress(roadmapId);
    }
}
