package com.test.test.service;


import com.test.test.dto.TaskRequestDTO;
import com.test.test.dto.TaskResponseDTO;
import com.test.test.dto.UniqueCodeGenerator;
import com.test.test.entity.Task;
import com.test.test.exeception.ResourceNotFoundException;
import com.test.test.exeception.ValidationException;
import com.test.test.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for task management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UniqueCodeGenerator codeGenerator;

    private static final int MAX_HIERARCHY_LEVEL = 5;


    @Override
    public TaskResponseDTO createTask(TaskRequestDTO requestDTO) {
        log.debug("Creating new task with title: {}", requestDTO.getTitle());

        // Generate unique code
        String code = codeGenerator.generateCode();
        log.debug("Generated code: {}", code);

        Task parent = null;
        if (requestDTO.getParentCode() != null && !requestDTO.getParentCode().isEmpty()) {
            parent = taskRepository.findByCode(requestDTO.getParentCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent task not found with code: " + requestDTO.getParentCode()
                    ));

            int parentLevel = parent.getHierarchyLevel();
            if (parentLevel >= MAX_HIERARCHY_LEVEL) {
                throw new ValidationException(
                        "Cannot create task: Maximum hierarchy level (" + MAX_HIERARCHY_LEVEL + ") reached"
                );
            }
        }

        Task task = Task.builder()
                .code(code)
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .status(requestDTO.getStatus())
                .assignedDate(requestDTO.getAssignedDate())
                .dueDate(requestDTO.getDueDate())
                .createdId(requestDTO.getCreatorId())
                .assignedId(requestDTO.getAssigneeId())
                .parent(parent)
                .priority(requestDTO.getPriority())
                .tags(requestDTO.getTags())
                .build();

        codeGenerator.registerCode(code);

        // Save task
        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with code: {}", savedTask.getCode());

        taskRepository.flush();
        
        // Reload the saved task with children fetched
        Task taskWithChildren = taskRepository.findByCode(savedTask.getCode())
                .orElse(savedTask);
        
        log.info("Task created successfully with code: {}", taskWithChildren.getCode());

        return convertToDTO(taskWithChildren);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDTO getTaskByCode(String code) {
        log.debug("Fetching task with code: {}", code);
        Task task = taskRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with code: " + code));
        return convertToDTO(task);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponseDTO> getAllTasks(Pageable pageable) {
        log.debug("Fetching all tasks with pagination: {}", pageable);
        return taskRepository.findAll(pageable)
                .map(this::convertToDTO);
    }


    @Override
    public TaskResponseDTO updateTask(String code, TaskRequestDTO requestDTO) {
        log.debug("Updating task with code: {}", code);

        Task task = taskRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with code: " + code));

        // Validate hierarchy if parent is being changed
        if (requestDTO.getParentCode() != null && !requestDTO.getParentCode().isEmpty()) {
            if (!requestDTO.getParentCode().equals(task.getCode())) {
                Task newParent = taskRepository.findByCode(requestDTO.getParentCode())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Parent task not found with code: " + requestDTO.getParentCode()
                        ));

                // Prevent circular reference
                if (isCircularReference(task, newParent)) {
                    throw new ValidationException("Cannot set parent: Circular reference detected");
                }

                // Validate hierarchy level
                int newParentLevel = newParent.getHierarchyLevel();
                if (newParentLevel >= MAX_HIERARCHY_LEVEL) {
                    throw new ValidationException(
                            "Cannot update task: Maximum hierarchy level (" + MAX_HIERARCHY_LEVEL + ") reached"
                    );
                }

                task.setParent(newParent);
            }
        } else if (requestDTO.getParentCode() == null || requestDTO.getParentCode().isEmpty()) {
            task.setParent(null);
        }

        // Update task fields
        task.setTitle(requestDTO.getTitle());
        task.setDescription(requestDTO.getDescription());
        task.setStatus(requestDTO.getStatus());
        task.setAssignedDate(requestDTO.getAssignedDate());
        task.setDueDate(requestDTO.getDueDate());
        task.setAssignedId(requestDTO.getAssigneeId());
        task.setPriority(requestDTO.getPriority());
        task.setTags(requestDTO.getTags());

        Task updatedTask = taskRepository.save(task);
        log.info("Task updated successfully with code: {}", updatedTask.getCode());

        taskRepository.flush();
        
        Task taskWithChildren = taskRepository.findByCode(updatedTask.getCode())
                .orElse(updatedTask);

        return convertToDTO(taskWithChildren);
    }


    @Override
    public void deleteTask(String code) {
        log.debug("Deleting task with code: {}", code);

        Task task = taskRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with code: " + code));

        List<Task> children = taskRepository.findByParentCode(code);
        if (!children.isEmpty()) {
            throw new ValidationException(
                    "Cannot delete task: Task has " + children.size() + " child task(s). " +
                            "Please delete or reassign child tasks first."
            );
        }

        taskRepository.delete(task);
        log.info("Task deleted successfully with code: {}", code);
    }


    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getChildTasks(String parentCode) {
        log.debug("Fetching child tasks for parent code: {}", parentCode);

        // Verify parent exists
        taskRepository.findByCode(parentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Parent task not found with code: " + parentCode));

        List<Task> children = taskRepository.findByParentCode(parentCode);
        return children.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getRootTasks() {
        log.debug("Fetching all root tasks");
        return taskRepository.findRootTasks().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    private TaskResponseDTO convertToDTO(Task task) {
        TaskResponseDTO.TaskResponseDTOBuilder builder = TaskResponseDTO.builder()
                .id(task.getId())
                .code(task.getCode())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignedDate(task.getAssignedDate())
                .dueDate(task.getDueDate())
                .creatorId(task.getCreatedId())
                .assigneeId(task.getAssignedId())
                .priority(task.getPriority())
                .tags(task.getTags())
                .createdAt(task.getCreateAt())
                .updatedAt(task.getUpdatedAt())
                .hierarchyLevel(task.getHierarchyLevel());

        if (task.getParent() != null) {
            builder.parentCode(task.getParent().getCode());
        }

        if (task.getChildren() != null && !task.getChildren().isEmpty()) {
            List<String> childCodes = task.getChildren().stream()
                    .map(Task::getCode)
                    .collect(Collectors.toList());
            builder.childCodes(childCodes);
        }

        return builder.build();
    }


    private boolean isCircularReference(Task task, Task newParent) {
        Task current = newParent;
        while (current != null) {
            if (current.getCode().equals(task.getCode())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}