package com.test.test.dto;

import com.test.test.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for task response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponseDTO {
    
    private Long id;
    private String code;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime assignedDate;
    private LocalDateTime dueDate;
    private Long creatorId;
    private Long assigneeId;
    private String parentCode;
    private String priority;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int hierarchyLevel;
    @Builder.Default
    private List<String> childCodes = new ArrayList<>();
}