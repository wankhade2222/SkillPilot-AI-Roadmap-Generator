package com.skillforge.repository;

import com.skillforge.entity.AiRoadmap;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRoadmapRepository extends JpaRepository<AiRoadmap, Long> {

    Optional<AiRoadmap> findByIdAndUserId(Long id, Long userId);

    List<AiRoadmap> findByUserIdOrderByCreatedAtDesc(Long userId);
}
