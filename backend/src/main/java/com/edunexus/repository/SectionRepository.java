package com.edunexus.repository;

import com.edunexus.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Integer> {
    List<Section> findByCourseIdOrderByOrderIndex(Integer courseId);
}
