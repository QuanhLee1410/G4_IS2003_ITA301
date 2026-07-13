package com.edunexus.controller;

import com.edunexus.dto.CourseDTO;
import com.edunexus.security.UserPrincipal;
import com.edunexus.service.CourseService;
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
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@Slf4j
@Tag(name = "Courses", description = "Course management endpoints")
public class CourseController {

    @Autowired
    private CourseService courseService;

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Create a new course", description = "Only teachers and admins can create courses")
    public ResponseEntity<CourseDTO> createCourse(@Valid @RequestBody CourseDTO courseDTO) {
        UserPrincipal user = getCurrentUser();
        log.info("Creating course: {} by user: {}", courseDTO.getTitle(), user.getUsername());
        CourseDTO createdCourse = courseService.createCourse(user.getUserId(), courseDTO);
        return new ResponseEntity<>(createdCourse, HttpStatus.CREATED);
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update a course", description = "Only course owner can update")
    public ResponseEntity<CourseDTO> updateCourse(
            @PathVariable Integer courseId,
            @Valid @RequestBody CourseDTO courseDTO) {
        UserPrincipal user = getCurrentUser();
        log.info("Updating course: {} by user: {}", courseId, user.getUsername());
        CourseDTO updatedCourse = courseService.updateCourse(courseId, user.getUserId(), courseDTO);
        return new ResponseEntity<>(updatedCourse, HttpStatus.OK);
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete a course", description = "Only course owner can delete")
    public ResponseEntity<?> deleteCourse(@PathVariable Integer courseId) {
        UserPrincipal user = getCurrentUser();
        log.info("Deleting course: {} by user: {}", courseId, user.getUsername());
        courseService.deleteCourse(courseId, user.getUserId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Get course details", description = "Get detailed information about a course including sections and lessons")
    public ResponseEntity<CourseDTO> getCourse(@PathVariable Integer courseId) {
        log.info("Getting course: {}", courseId);
        CourseDTO course = courseService.getCourseById(courseId);
        return new ResponseEntity<>(course, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "List all courses", description = "Get list of all available courses")
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        log.info("Getting all courses");
        List<CourseDTO> courses = courseService.getAllCourses();
        return new ResponseEntity<>(courses, HttpStatus.OK);
    }

    @GetMapping("/teacher/{teacherId}")
    @Operation(summary = "List teacher's courses", description = "Get list of courses created by a specific teacher")
    public ResponseEntity<List<CourseDTO>> getTeacherCourses(@PathVariable Integer teacherId) {
        log.info("Getting courses for teacher: {}", teacherId);
        List<CourseDTO> courses = courseService.getCoursesByTeacher(teacherId);
        return new ResponseEntity<>(courses, HttpStatus.OK);
    }
}
