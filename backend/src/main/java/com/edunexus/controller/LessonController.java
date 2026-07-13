package com.edunexus.controller;

import com.edunexus.entity.Lesson;
import com.edunexus.security.UserPrincipal;
import com.edunexus.service.EnrollmentService;
import com.edunexus.service.LessonService;
import com.edunexus.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lessons")
@Slf4j
@Tag(name = "Lessons", description = "Lesson management and video streaming endpoints")
public class LessonController {

    @Autowired
    private LessonService lessonService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private S3Service s3Service;

    private UserPrincipal getCurrentUser() {
        try {
            return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Create a new lesson", description = "Add a lesson to a section")
    public ResponseEntity<Lesson> createLesson(
            @PathVariable Integer sectionId,
            @Valid @RequestBody Lesson lesson) {
        log.info("Creating lesson in section: {}", sectionId);
        Lesson createdLesson = lessonService.createLesson(sectionId, lesson);
        return new ResponseEntity<>(createdLesson, HttpStatus.CREATED);
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update a lesson", description = "Modify lesson details")
    public ResponseEntity<Lesson> updateLesson(
            @PathVariable Integer lessonId,
            @Valid @RequestBody Lesson lessonDetails) {
        log.info("Updating lesson: {}", lessonId);
        Lesson updatedLesson = lessonService.updateLesson(lessonId, lessonDetails);
        return new ResponseEntity<>(updatedLesson, HttpStatus.OK);
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete a lesson", description = "Remove a lesson")
    public ResponseEntity<?> deleteLesson(@PathVariable Integer lessonId) {
        log.info("Deleting lesson: {}", lessonId);
        lessonService.deleteLesson(lessonId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{lessonId}")
    @Operation(summary = "Get lesson details", description = "Get lesson information and access video")
    public ResponseEntity<Lesson> getLesson(@PathVariable Integer lessonId) {
        log.info("Getting lesson: {}", lessonId);
        Lesson lesson = lessonService.getLessonById(lessonId);
        return new ResponseEntity<>(lesson, HttpStatus.OK);
    }

    @GetMapping("/section/{sectionId}")
    @Operation(summary = "List section lessons", description = "Get all lessons in a section ordered by index")
    public ResponseEntity<List<Lesson>> getLessonsBySection(@PathVariable Integer sectionId) {
        log.info("Getting lessons for section: {}", sectionId);
        List<Lesson> lessons = lessonService.getLessonsBySection(sectionId);
        return new ResponseEntity<>(lessons, HttpStatus.OK);
    }

    @GetMapping("/{lessonId}/video")
    @Operation(summary = "Get video presigned URL", description = "Get S3 presigned URL to stream video")
    public ResponseEntity<?> getVideoPresignedUrl(@PathVariable Integer lessonId) {
        log.info("Getting video presigned URL for lesson: {}", lessonId);
        
        Lesson lesson = lessonService.getLessonById(lessonId);
        
        if (lesson.getVideoUrl() == null || lesson.getVideoUrl().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No video available for this lesson"));
        }

        if (!lesson.getIsPreview()) {
            UserPrincipal user = getCurrentUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Must be logged in to access this video"));
            }

            if (!enrollmentService.isStudentEnrolled(user.getUserId(), lesson.getSection().getCourse().getCourseId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Student must be enrolled in the course to access this video"));
            }
        }

        String videoUrl = lesson.getVideoUrl();
        String presignedUrl;

        if (videoUrl.startsWith("http")) {
            presignedUrl = videoUrl;
        } else {
            presignedUrl = s3Service.generatePresignedUrl(videoUrl);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("videoUrl", presignedUrl);
        response.put("duration", lesson.getDurationSeconds());
        response.put("title", lesson.getTitle());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{lessonId}/video-url")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update video URL", description = "Update the S3 video URL for a lesson")
    public ResponseEntity<?> updateVideoUrl(
            @PathVariable Integer lessonId,
            @RequestBody Map<String, String> request) {
        log.info("Updating video URL for lesson: {}", lessonId);
        String videoUrl = request.get("videoUrl");
        lessonService.updateLessonVideoUrl(lessonId, videoUrl);
        return new ResponseEntity<>(Map.of("message", "Video URL updated successfully"), HttpStatus.OK);
    }
}
