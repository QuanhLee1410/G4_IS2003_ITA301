package com.edunexus.service;

import com.edunexus.entity.Lesson;
import com.edunexus.entity.Section;
import com.edunexus.exception.AppException;
import com.edunexus.exception.ResourceNotFoundException;
import com.edunexus.repository.LessonRepository;
import com.edunexus.repository.SectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LessonService {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private SectionRepository sectionRepository;

    public Lesson createLesson(Integer sectionId, Lesson lesson) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        lesson.setSection(section);
        lesson = lessonRepository.save(lesson);
        log.info("Lesson created: {} in section: {}", lesson.getLessonId(), sectionId);

        return lesson;
    }

    public Lesson updateLesson(Integer lessonId, Lesson lessonDetails) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        lesson.setTitle(lessonDetails.getTitle());
        lesson.setContentType(lessonDetails.getContentType());
        lesson.setVideoUrl(lessonDetails.getVideoUrl());
        lesson.setDocumentUrl(lessonDetails.getDocumentUrl());
        lesson.setDurationSeconds(lessonDetails.getDurationSeconds());
        lesson.setOrderIndex(lessonDetails.getOrderIndex());
        lesson.setIsPreview(lessonDetails.getIsPreview());

        lesson = lessonRepository.save(lesson);
        log.info("Lesson updated: {}", lessonId);

        return lesson;
    }

    public void deleteLesson(Integer lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        lessonRepository.delete(lesson);
        log.info("Lesson deleted: {}", lessonId);
    }

    public Lesson getLessonById(Integer lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
    }

    public List<Lesson> getLessonsBySection(Integer sectionId) {
        return lessonRepository.findBySectionIdOrderByOrderIndex(sectionId);
    }

    public void updateLessonVideoUrl(Integer lessonId, String videoUrl) {
        Lesson lesson = getLessonById(lessonId);
        lesson.setVideoUrl(videoUrl);
        lessonRepository.save(lesson);
        log.info("Lesson {} video URL updated", lessonId);
    }
}
