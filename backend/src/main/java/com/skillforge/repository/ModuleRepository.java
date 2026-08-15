package com.skillforge.repository;

import com.skillforge.entity.Module;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByRoadmapIdOrderByModuleOrderAsc(Long roadmapId);

    long countByRoadmapId(Long roadmapId);

    long countByRoadmapIdAndCompletedTrue(Long roadmapId);
}
