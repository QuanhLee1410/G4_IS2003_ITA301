package com.edunexus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Integer courseId;

    @NotBlank(message = "Course title is required")
    private String title;

    private String description;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    private String thumbnailUrl;

    private Integer teacherId;

    private String teacherName;

    private List<SectionDTO> sections;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SectionDTO {
    private Integer sectionId;
    private String title;
    private Integer orderIndex;
    private List<LessonDTO> lessons;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LessonDTO {
    private Integer lessonId;
    private String title;
    private String contentType;
    private String videoUrl;
    private String documentUrl;
    private Integer durationSeconds;
    private Integer orderIndex;
    private Boolean isPreview;
}
