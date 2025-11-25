package com.test.test.dto;

import com.test.test.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for creating and updating tasks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequestDTO {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Status is required")
    private TaskStatus status;
    
    private LocalDateTime assignedDate;
    
    private LocalDateTime dueDate;
    
    @NotNull(message = "Creator ID is required")
    private Long creatorId;
    
    private Long assigneeId;
    private String parentCode;

    private String priority;

    private String tags;
}