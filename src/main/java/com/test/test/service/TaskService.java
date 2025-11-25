package com.test.test.service;

import com.test.test.dto.TaskRequestDTO;
import com.test.test.dto.TaskResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for task management operations
 */
public interface TaskService {

    TaskResponseDTO createTask(TaskRequestDTO requestDTO);
    TaskResponseDTO getTaskByCode(String code);

    Page<TaskResponseDTO> getAllTasks(Pageable pageable);

    TaskResponseDTO updateTask(String code, TaskRequestDTO requestDTO);

    void deleteTask(String code);

    List<TaskResponseDTO> getChildTasks(String parentCode);

    List<TaskResponseDTO> getRootTasks();
}