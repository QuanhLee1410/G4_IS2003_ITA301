package com.edunexus.controller;

import com.edunexus.entity.Section;
import com.edunexus.security.UserPrincipal;
import com.edunexus.service.SectionService;
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
@RequestMapping("/api/courses/{courseId}/sections")
@Slf4j
@Tag(name = "Sections", description = "Course section management endpoints")
public class SectionController {

    @Autowired
    private SectionService sectionService;

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Create a new section", description = "Add a new section to a course")
    public ResponseEntity<Section> createSection(
            @PathVariable Integer courseId,
            @Valid @RequestBody Section section) {
        log.info("Creating section in course: {}", courseId);
        Section createdSection = sectionService.createSection(courseId, section);
        return new ResponseEntity<>(createdSection, HttpStatus.CREATED);
    }

    @PutMapping("/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update a section", description = "Modify section title and order")
    public ResponseEntity<Section> updateSection(
            @PathVariable Integer courseId,
            @PathVariable Integer sectionId,
            @Valid @RequestBody Section sectionDetails) {
        log.info("Updating section: {} in course: {}", sectionId, courseId);
        Section updatedSection = sectionService.updateSection(sectionId, sectionDetails);
        return new ResponseEntity<>(updatedSection, HttpStatus.OK);
    }

    @DeleteMapping("/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete a section", description = "Remove a section and all its lessons")
    public ResponseEntity<?> deleteSection(
            @PathVariable Integer courseId,
            @PathVariable Integer sectionId) {
        log.info("Deleting section: {} from course: {}", sectionId, courseId);
        sectionService.deleteSection(sectionId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping
    @Operation(summary = "List sections", description = "Get all sections of a course")
    public ResponseEntity<List<Section>> getSections(@PathVariable Integer courseId) {
        log.info("Getting sections for course: {}", courseId);
        List<Section> sections = sectionService.getSectionsByCourse(courseId);
        return new ResponseEntity<>(sections, HttpStatus.OK);
    }

    @GetMapping("/{sectionId}")
    @Operation(summary = "Get section details", description = "Get detailed information about a section")
    public ResponseEntity<Section> getSection(
            @PathVariable Integer courseId,
            @PathVariable Integer sectionId) {
        log.info("Getting section: {} from course: {}", sectionId, courseId);
        Section section = sectionService.getSectionById(sectionId);
        return new ResponseEntity<>(section, HttpStatus.OK);
    }
}
