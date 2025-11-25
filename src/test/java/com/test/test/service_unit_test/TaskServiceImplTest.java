package com.test.test.service_unit_test;

import com.test.test.dto.TaskRequestDTO;
import com.test.test.dto.TaskResponseDTO;
import com.test.test.dto.UniqueCodeGenerator;
import com.test.test.entity.Task;
import com.test.test.entity.TaskStatus;
import com.test.test.exeception.ResourceNotFoundException;
import com.test.test.exeception.ValidationException;
import com.test.test.repository.TaskRepository;
import com.test.test.service.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl Unit Tests")
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UniqueCodeGenerator codeGenerator;

    @InjectMocks
    private TaskServiceImpl taskService;

    private TaskRequestDTO taskRequestDTO;
    private Task task;
    private String testCode;

    @BeforeEach
    void setUp() {
        testCode = "AB-12-xyz1";
        
        taskRequestDTO = TaskRequestDTO.builder()
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .assignedDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(7))
                .creatorId(1L)
                .assigneeId(2L)
                .priority("HIGH")
                .tags("tag1,tag2")
                .build();

        task = Task.builder()
                .id(1L)
                .code(testCode)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .assignedDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(7))
                .createdId(1L)
                .assignedId(2L)
                .priority("HIGH")
                .tags("tag1,tag2")
                .createAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .children(new ArrayList<>())
                .build();
    }

    // ========== createTask Tests ==========

    @Test
    @DisplayName("Should create task successfully without parent")
    void createTask_WithoutParent_Success() {
        // Given
        when(codeGenerator.generateCode()).thenReturn(testCode);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        TaskResponseDTO result = taskService.createTask(taskRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals(testCode, result.getCode());
        assertEquals("Test Task", result.getTitle());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        assertEquals(1, result.getHierarchyLevel());
        assertNull(result.getParentCode());

        verify(codeGenerator).generateCode();
        verify(codeGenerator).registerCode(testCode);
        verify(taskRepository).save(any(Task.class));
        verify(taskRepository, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("Should create task successfully with valid parent")
    void createTask_WithValidParent_Success() {
        // Given
        String parentCode = "CD-34-abcd";
        Task parent = Task.builder()
                .id(2L)
                .code(parentCode)
                .title("Parent Task")
                .status(TaskStatus.PENDING)
                .build();

        taskRequestDTO.setParentCode(parentCode);
        task.setParent(parent);

        when(codeGenerator.generateCode()).thenReturn(testCode);
        when(taskRepository.findByCode(parentCode)).thenReturn(Optional.of(parent));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        TaskResponseDTO result = taskService.createTask(taskRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals(testCode, result.getCode());
        assertEquals(parentCode, result.getParentCode());

        verify(taskRepository).findByCode(parentCode);
        verify(codeGenerator).generateCode();
        verify(codeGenerator).registerCode(testCode);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when parent not found")
    void createTask_ParentNotFound_ThrowsException() {
        // Given
        String parentCode = "CD-34-abcd";
        taskRequestDTO.setParentCode(parentCode);

        when(codeGenerator.generateCode()).thenReturn(testCode);
        when(taskRepository.findByCode(parentCode)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.createTask(taskRequestDTO)
        );

        assertEquals("Parent task not found with code: " + parentCode, exception.getMessage());
        verify(taskRepository).findByCode(parentCode);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when max hierarchy level reached")
    void createTask_MaxHierarchyLevelReached_ThrowsException() {
        // Given
        String parentCode = "CD-34-abcd";
        Task parent = Task.builder()
                .id(2L)
                .code(parentCode)
                .title("Parent Task")
                .status(TaskStatus.PENDING)
                .build();
        // Create a chain of parents to reach max level
        Task level5Parent = parent;
        for (int i = 0; i < 4; i++) {
            Task tempParent = Task.builder()
                    .code("LVL-" + i)
                    .parent(level5Parent)
                    .build();
            level5Parent = tempParent;
        }
        parent = level5Parent;

        taskRequestDTO.setParentCode(parentCode);

        when(codeGenerator.generateCode()).thenReturn(testCode);
        when(taskRepository.findByCode(parentCode)).thenReturn(Optional.of(parent));

        // When & Then
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> taskService.createTask(taskRequestDTO)
        );

        assertTrue(exception.getMessage().contains("Maximum hierarchy level"));
        verify(taskRepository).findByCode(parentCode);
        verify(taskRepository, never()).save(any(Task.class));
    }

    // ========== getTaskByCode Tests ==========

    @Test
    @DisplayName("Should get task by code successfully")
    void getTaskByCode_ValidCode_Success() {
        // Given
        when(taskRepository.findByCode(testCode)).thenReturn(Optional.of(task));

        // When
        TaskResponseDTO result = taskService.getTaskByCode(testCode);

        // Then
        assertNotNull(result);
        assertEquals(testCode, result.getCode());
        assertEquals("Test Task", result.getTitle());
        verify(taskRepository).findByCode(testCode);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when task not found")
    void getTaskByCode_TaskNotFound_ThrowsException() {
        // Given
        when(taskRepository.findByCode(testCode)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.getTaskByCode(testCode)
        );

        assertEquals("Task not found with code: " + testCode, exception.getMessage());
        verify(taskRepository).findByCode(testCode);
    }

    // ========== getAllTasks Tests ==========

    @Test
    @DisplayName("Should get all tasks with pagination")
    void getAllTasks_WithPagination_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Task> tasks = List.of(task);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, 1);

        when(taskRepository.findAll(pageable)).thenReturn(taskPage);

        // When
        Page<TaskResponseDTO> result = taskService.getAllTasks(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(testCode, result.getContent().get(0).getCode());
        verify(taskRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should return empty page when no tasks exist")
    void getAllTasks_NoTasks_ReturnsEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(taskRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<TaskResponseDTO> result = taskService.getAllTasks(pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(taskRepository).findAll(pageable);
    }

    // ========== updateTask Tests ==========


    @Test
    @DisplayName("Should update task successfully with new parent")
    void updateTask_WithNewParent_Success() {
        // Given
        String newParentCode = "EF-56-efgh";
        Task newParent = Task.builder()
                .id(3L)
                .code(newParentCode)
                .title("New Parent")
                .status(TaskStatus.PENDING)
                .build();

        taskRequestDTO.setParentCode(newParentCode);

        when(taskRepository.findByCode(testCode)).thenReturn(Optional.of(task));
        when(taskRepository.findByCode(newParentCode)).thenReturn(Optional.of(newParent));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        TaskResponseDTO result = taskService.updateTask(testCode, taskRequestDTO);

        // Then
        assertNotNull(result);
        verify(taskRepository).findByCode(testCode);
        verify(taskRepository).findByCode(newParentCode);
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("Should remove parent when parentCode is empty")
    void updateTask_RemoveParent_Success() {
        // Given
        task.setParent(Task.builder().code("OLD-PARENT").build());
        taskRequestDTO.setParentCode("");

        when(taskRepository.findByCode(testCode)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        TaskResponseDTO result = taskService.updateTask(testCode, taskRequestDTO);

        // Then
        assertNotNull(result);
        verify(taskRepository).findByCode(testCode);
        verify(taskRepository).save(task);
        assertNull(task.getParent());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when task not found for update")
    void updateTask_TaskNotFound_ThrowsException() {
        // Given
        when(taskRepository.findByCode(testCode)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.updateTask(testCode, taskRequestDTO)
        );

        assertEquals("Task not found with code: " + testCode, exception.getMessage());
        verify(taskRepository).findByCode(testCode);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when circular reference detected")
    void updateTask_CircularReference_ThrowsException() {
        // Given
        String childCode = "CH-78-child";
        Task child = Task.builder()
                .id(4L)
                .code(childCode)
                .parent(task)
                .build();
        task.setParent(child); // Create circular reference
        taskRequestDTO.setParentCode(childCode);

        when(taskRepository.findByCode(testCode)).thenReturn(Optional.of(task));
        when(taskRepository.findByCode(childCode)).thenReturn(Optional.of(child));

        // When & Then
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> taskService.updateTask(testCode, taskRequestDTO)
        );

        assertEquals("Cannot set parent: Circular reference detected", exception.getMessage());
        verify(taskRepository).findByCode(testCode);
        verify(taskRepository).findByCode(childCode);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when new parent at max hierarchy level")
    void updateTask_NewParentAtMaxLevel_ThrowsException() {
        // Given
        String newParentCode = "EF-56-efgh";
        Task newParent = Task.builder()
                .code(newParentCode)
                .build();
        
        // Create a chain to reach max level
        Task current = newParent;
        for (int i = 0; i < 4; i++) {
            Task tempParent = Task.builder()
                    .code("MAX-" + i)
                    .parent(current)
                    .build();
            current = tempParent;
        }
        newParent = current;

        taskRequestDTO.setParentCode(newParentCode);

        when(taskRepository.findByCode(testCode)).thenReturn(Optional.of(task));
        when(taskRepository.findByCode(newParentCode)).thenReturn(Optional.of(newParent));

        // When & Then
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> taskService.updateTask(testCode, taskRequestDTO)
        );

        assertTrue(exception.getMessage().contains("Maximum hierarchy level"));
        verify(taskRepository).findByCode(testCode);
        verify(taskRepository).findByCode(newParentCode);
        verify(taskRepository, never()).save(any(Task.class));
    }


    // ========== deleteTask Tests ==========

    @Test
    @DisplayName("Should delete task successfully when no children")
    void deleteTask_NoChildren_Success() {
        // Given
        when(taskRepository.findByCode(testCode)).thenReturn(Optional.of(task));

        // When
        taskService.deleteTask(testCode);

        // Then
        verify(taskRepository).findByCode(testCode);
        verify(taskRepository).delete(task);
    }

    @Test
    @DisplayName("Should throw ValidationException when task has children")
    void deleteTask_WithChildren_ThrowsException() {
        // Given
        Task child = Task.builder()
                .code("CH-00-child")
                .parent(task)
                .build();
        task.getChildren().add(child);

        when(taskRepository.findByCode(testCode)).thenReturn(Optional.of(task));

        // When & Then
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> taskService.deleteTask(testCode)
        );

        assertTrue(exception.getMessage().contains("child task"));
        verify(taskRepository).findByCode(testCode);
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when task not found for deletion")
    void deleteTask_TaskNotFound_ThrowsException() {
        // Given
        when(taskRepository.findByCode(testCode)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.deleteTask(testCode)
        );

        assertEquals("Task not found with code: " + testCode, exception.getMessage());
        verify(taskRepository).findByCode(testCode);
        verify(taskRepository, never()).delete(any(Task.class));
    }

    // ========== getChildTasks Tests ==========

    @Test
    @DisplayName("Should get child tasks successfully")
    void getChildTasks_ValidParent_Success() {
        // Given
        String parentCode = "PAR-99-parent";
        Task parent = Task.builder()
                .id(5L)
                .code(parentCode)
                .build();

        Task child1 = Task.builder()
                .id(6L)
                .code("CH-01-child1")
                .parent(parent)
                .status(TaskStatus.PENDING)
                .build();

        Task child2 = Task.builder()
                .id(7L)
                .code("CH-02-child2")
                .parent(parent)
                .status(TaskStatus.IN_PROGRESS)
                .build();

        List<Task> children = List.of(child1, child2);

        when(taskRepository.findByCode(parentCode)).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentCode(parentCode)).thenReturn(children);

        // When
        List<TaskResponseDTO> result = taskService.getChildTasks(parentCode);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("CH-01-child1", result.get(0).getCode());
        assertEquals("CH-02-child2", result.get(1).getCode());
        verify(taskRepository).findByCode(parentCode);
        verify(taskRepository).findByParentCode(parentCode);
    }

    @Test
    @DisplayName("Should return empty list when no child tasks")
    void getChildTasks_NoChildren_ReturnsEmptyList() {
        // Given
        String parentCode = "PAR-99-parent";
        Task parent = Task.builder()
                .code(parentCode)
                .build();

        when(taskRepository.findByCode(parentCode)).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentCode(parentCode)).thenReturn(Collections.emptyList());

        // When
        List<TaskResponseDTO> result = taskService.getChildTasks(parentCode);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository).findByCode(parentCode);
        verify(taskRepository).findByParentCode(parentCode);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when parent not found")
    void getChildTasks_ParentNotFound_ThrowsException() {
        // Given
        String parentCode = "PAR-99-parent";
        when(taskRepository.findByCode(parentCode)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.getChildTasks(parentCode)
        );

        assertEquals("Parent task not found with code: " + parentCode, exception.getMessage());
        verify(taskRepository).findByCode(parentCode);
        verify(taskRepository, never()).findByParentCode(anyString());
    }

    // ========== getRootTasks Tests ==========

    @Test
    @DisplayName("Should get root tasks successfully")
    void getRootTasks_WithRoots_Success() {
        // Given
        Task root1 = Task.builder()
                .id(8L)
                .code("RT-11-root1")
                .title("Root Task 1")
                .status(TaskStatus.PENDING)
                .build();

        Task root2 = Task.builder()
                .id(9L)
                .code("RT-22-root2")
                .title("Root Task 2")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        List<Task> rootTasks = List.of(root1, root2);

        when(taskRepository.findRootTasks()).thenReturn(rootTasks);

        // When
        List<TaskResponseDTO> result = taskService.getRootTasks();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("RT-11-root1", result.get(0).getCode());
        assertEquals("RT-22-root2", result.get(1).getCode());
        verify(taskRepository).findRootTasks();
    }

    @Test
    void getRootTasks_NoRoots_ReturnsEmptyList() {
        // Given
        when(taskRepository.findRootTasks()).thenReturn(Collections.emptyList());

        // When
        List<TaskResponseDTO> result = taskService.getRootTasks();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository).findRootTasks();
    }

    // ========== convertToDTO Helper Tests (indirectly tested) ==========

    @Test
    @DisplayName("Should convert task to DTO with parent and children")
    void convertToDTO_WithParentAndChildren_Success() {
        // Given
        Task parent = Task.builder()
                .code("PAR-88-parent")
                .build();

        Task child1 = Task.builder()
                .code("CH-11-child1")
                .build();

        Task child2 = Task.builder()
                .code("CH-22-child2")
                .build();

        task.setParent(parent);
        task.getChildren().add(child1);
        task.getChildren().add(child2);

        when(taskRepository.findByCode(testCode)).thenReturn(Optional.of(task));

        // When
        TaskResponseDTO result = taskService.getTaskByCode(testCode);

        // Then
        assertNotNull(result);
        assertEquals("PAR-88-parent", result.getParentCode());
        assertNotNull(result.getChildCodes());
        assertEquals(2, result.getChildCodes().size());
        assertTrue(result.getChildCodes().contains("CH-11-child1"));
        assertTrue(result.getChildCodes().contains("CH-22-child2"));
    }

    @Test
    @DisplayName("Should convert task to DTO without parent and children")
    void convertToDTO_WithoutParentAndChildren_Success() {
        // Given
        task.setParent(null);
        task.setChildren(new ArrayList<>());

        when(taskRepository.findByCode(testCode)).thenReturn(Optional.of(task));

        // When
        TaskResponseDTO result = taskService.getTaskByCode(testCode);

        // Then
        assertNotNull(result);
        assertNull(result.getParentCode());
        assertNotNull(result.getChildCodes());
        assertTrue(result.getChildCodes().isEmpty());
    }
}