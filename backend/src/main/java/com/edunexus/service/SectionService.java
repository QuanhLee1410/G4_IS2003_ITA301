package com.edunexus.service;

import com.edunexus.entity.Course;
import com.edunexus.entity.Section;
import com.edunexus.exception.ResourceNotFoundException;
import com.edunexus.repository.CourseRepository;
import com.edunexus.repository.SectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SectionService {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    public Section createSection(Integer courseId, Section section) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        section.setCourse(course);
        section = sectionRepository.save(section);
        log.info("Section created: {} in course: {}", section.getSectionId(), courseId);

        return section;
    }

    public Section updateSection(Integer sectionId, Section sectionDetails) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        section.setTitle(sectionDetails.getTitle());
        section.setOrderIndex(sectionDetails.getOrderIndex());

        section = sectionRepository.save(section);
        log.info("Section updated: {}", sectionId);

        return section;
    }

    public void deleteSection(Integer sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        sectionRepository.delete(section);
        log.info("Section deleted: {}", sectionId);
    }

    public Section getSectionById(Integer sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));
    }

    public List<Section> getSectionsByCourse(Integer courseId) {
        return sectionRepository.findByCourseIdOrderByOrderIndex(courseId);
    }
}
