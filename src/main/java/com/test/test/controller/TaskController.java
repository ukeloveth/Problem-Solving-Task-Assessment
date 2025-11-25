package com.test.test.controller;

import com.test.test.dto.TaskRequestDTO;
import com.test.test.dto.TaskResponseDTO;
import com.test.test.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Task Management API
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;

    /**
     * Create a new task
     */
    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(@Valid @RequestBody TaskRequestDTO taskRequest) {
        log.info("Received request to create task: {}", taskRequest.getTitle());
        TaskResponseDTO createdTask = taskService.createTask(taskRequest);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    /**
     * Get task by code
     */
    @GetMapping("/{code}")
    public ResponseEntity<TaskResponseDTO> getTask(@PathVariable String code) {
        log.info("Received request to get task with code: {}", code);
        TaskResponseDTO task = taskService.getTaskByCode(code);
        return ResponseEntity.ok(task);
    }

    /**
     * Get all tasks with pagination
     */
    @GetMapping
    public ResponseEntity<Page<TaskResponseDTO>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Received request to get all tasks - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TaskResponseDTO> tasks = taskService.getAllTasks(pageable);

        return ResponseEntity.ok(tasks);
    }

    /**
     * Update task by code
     */
    @PutMapping("/{code}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable String code,
            @Valid @RequestBody TaskRequestDTO taskRequest) {

        log.info("Received request to update task with code: {}", code);
        TaskResponseDTO updatedTask = taskService.updateTask(code, taskRequest);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Delete task by code
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteTask(@PathVariable String code) {
        log.info("Received request to delete task with code: {}", code);
        taskService.deleteTask(code);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all child tasks of a parent task
     */
    @GetMapping("/{code}/children")
    public ResponseEntity<List<TaskResponseDTO>> getChildTasks(@PathVariable String code) {
        log.info("Received request to get children of task with code: {}", code);
        List<TaskResponseDTO> children = taskService.getChildTasks(code);
        return ResponseEntity.ok(children);
    }

    /**
     * Get all root tasks (tasks without parent)
     */
    @GetMapping("/root")
    public ResponseEntity<List<TaskResponseDTO>> getRootTasks() {
        log.info("Received request to get all root tasks");
        List<TaskResponseDTO> rootTasks = taskService.getRootTasks();
        return ResponseEntity.ok(rootTasks);
    }
}