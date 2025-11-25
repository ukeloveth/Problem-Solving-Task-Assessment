package com.test.test.controller_integration_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.test.dto.TaskRequestDTO;
import com.test.test.dto.TaskResponseDTO;
import com.test.test.entity.Task;
import com.test.test.entity.TaskStatus;
import com.test.test.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TaskController Integration Tests")
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    private TaskRequestDTO validTaskRequest;
    private Task savedTask;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();

        validTaskRequest = TaskRequestDTO.builder()
                .title("Integration Test Task")
                .description("This is a test task for integration testing")
                .status(TaskStatus.PENDING)
                .assignedDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(7))
                .creatorId(1L)
                .assigneeId(2L)
                .priority("HIGH")
                .tags("integration,test")
                .build();

        savedTask = Task.builder()
                .code("TS-01-test1")
                .title("Existing Task")
                .description("An existing task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .assignedId(2L)
                .priority("MEDIUM")
                .tags("existing")
                .children(new ArrayList<>())
                .build();
        savedTask = taskRepository.save(savedTask);
    }

    // ========== POST /api/tasks - Create Task Tests ==========

    @Test
    @DisplayName("Should create task successfully")
    void createTask_ValidRequest_ReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTaskRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Integration Test Task"))
                .andExpect(jsonPath("$.description").value("This is a test task for integration testing"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.id").exists());

        assertEquals(2, taskRepository.count());
    }

    @Test
    @DisplayName("Should create task with parent successfully")
    void createTask_WithParent_ReturnsCreated() throws Exception {
        validTaskRequest.setParentCode(savedTask.getCode());

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTaskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentCode").value(savedTask.getCode()))
                .andExpect(jsonPath("$.hierarchyLevel").value(2));
    }

    @Test
    @DisplayName("Should return 400 when title is missing")
    void createTask_MissingTitle_ReturnsBadRequest() throws Exception {
        validTaskRequest.setTitle(null);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTaskRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when status is missing")
    void createTask_MissingStatus_ReturnsBadRequest() throws Exception {
        validTaskRequest.setStatus(null);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTaskRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when parent task not found")
    void createTask_ParentNotFound_ReturnsNotFound() throws Exception {
        validTaskRequest.setParentCode("NON-EXISTENT");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTaskRequest)))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/tasks/{code} - Get Task Tests ==========

    @Test
    @DisplayName("Should get task by code successfully")
    void getTask_ValidCode_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/tasks/{code}", savedTask.getCode()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(savedTask.getCode()))
                .andExpect(jsonPath("$.title").value("Existing Task"))
                .andExpect(jsonPath("$.description").value("An existing task"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").value(savedTask.getId().intValue()));
    }

    @Test
    @DisplayName("Should return 404 when task not found")
    void getTask_InvalidCode_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/{code}", "NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/tasks - Get All Tasks Tests ==========

    @Test
    @DisplayName("Should get all tasks with default pagination")
    void getAllTasks_DefaultPagination_ReturnsOk() throws Exception {
        for (int i = 0; i < 5; i++) {
            Task task = Task.builder()
                    .code("TS-0" + i + "-task" + i)
                    .title("Task " + i)
                    .description("Description " + i)
                    .status(TaskStatus.PENDING)
                    .createdId(1L)
                    .children(new ArrayList<>())
                    .build();
            taskRepository.save(task);
        }

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(6)) // 1 from setUp + 5 new
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("Should get tasks with custom pagination")
    void getAllTasks_CustomPagination_ReturnsPaginatedResults() throws Exception {
        // Create 15 tasks
        for (int i = 0; i < 15; i++) {
            Task task = Task.builder()
                    .code("TS-" + String.format("%02d", i) + "-task" + i)
                    .title("Task " + i)
                    .description("Description " + i)
                    .status(TaskStatus.PENDING)
                    .createdId(1L)
                    .children(new ArrayList<>())
                    .build();
            taskRepository.save(task);
        }

        mockMvc.perform(get("/api/tasks")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(16)) // 1 from setUp + 15 new
                .andExpect(jsonPath("$.totalPages").value(4))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    @DisplayName("Should get tasks with sorting")
    void getAllTasks_WithSorting_ReturnsSortedResults() throws Exception {
        Task taskA = Task.builder()
                .code("TS-AA-task1")
                .title("Alpha Task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .children(new ArrayList<>())
                .build();

        Task taskB = Task.builder()
                .code("TS-BB-task2")
                .title("Beta Task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .children(new ArrayList<>())
                .build();

        taskRepository.save(taskA);
        taskRepository.save(taskB);

        mockMvc.perform(get("/api/tasks")
                        .param("sortBy", "title")
                        .param("sortDir", "asc")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Alpha Task"))
                .andExpect(jsonPath("$.content[1].title").value("Beta Task"));
    }

    @Test
    @DisplayName("Should return empty page when no tasks exist")
    void getAllTasks_NoTasks_ReturnsEmptyPage() throws Exception {
        taskRepository.deleteAll();

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ========== PUT /api/tasks/{code} - Update Task Tests ==========

    @Test
    @DisplayName("Should update task successfully")
    void updateTask_ValidRequest_ReturnsOk() throws Exception {
        TaskRequestDTO updateRequest = TaskRequestDTO.builder()
                .title("Updated Task Title")
                .description("Updated description")
                .status(TaskStatus.IN_PROGRESS)
                .assignedDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(5))
                .creatorId(1L)
                .assigneeId(3L)
                .priority("LOW")
                .tags("updated,tags")
                .build();

        mockMvc.perform(put("/api/tasks/{code}", savedTask.getCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(savedTask.getCode()))
                .andExpect(jsonPath("$.title").value("Updated Task Title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("LOW"));

        Task updatedTask = taskRepository.findByCode(savedTask.getCode()).orElseThrow();
        assertEquals("Updated Task Title", updatedTask.getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus());
    }

    @Test
    @DisplayName("Should update task with new parent")
    void updateTask_WithNewParent_ReturnsOk() throws Exception {
        Task parent = Task.builder()
                .code("PR-01-parent")
                .title("Parent Task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .children(new ArrayList<>())
                .build();
        taskRepository.save(parent);

        TaskRequestDTO updateRequest = TaskRequestDTO.builder()
                .title(savedTask.getTitle())
                .description(savedTask.getDescription())
                .status(savedTask.getStatus())
                .creatorId(1L)
                .parentCode(parent.getCode())
                .build();

        mockMvc.perform(put("/api/tasks/{code}", savedTask.getCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentCode").value(parent.getCode()));

        Task updatedTask = taskRepository.findByCode(savedTask.getCode()).orElseThrow();
        assertNotNull(updatedTask.getParent());
        assertEquals(parent.getCode(), updatedTask.getParent().getCode());
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent task")
    void updateTask_TaskNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/tasks/{code}", "NON-EXISTENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTaskRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when validation fails")
    void updateTask_InvalidRequest_ReturnsBadRequest() throws Exception {
        validTaskRequest.setTitle(null);

        mockMvc.perform(put("/api/tasks/{code}", savedTask.getCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTaskRequest)))
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE /api/tasks/{code} - Delete Task Tests ==========

    @Test
    @DisplayName("Should delete task successfully")
    void deleteTask_ValidCode_ReturnsNoContent() throws Exception {
        String taskCode = savedTask.getCode();

        mockMvc.perform(delete("/api/tasks/{code}", taskCode))
                .andExpect(status().isNoContent());

        assertFalse(taskRepository.findByCode(taskCode).isPresent());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent task")
    void deleteTask_TaskNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/tasks/{code}", "NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when deleting task with children")
    void deleteTask_WithChildren_ReturnsBadRequest() throws Exception {
        Task child = Task.builder()
                .code("CH-01-child")
                .title("Child Task")
                .description("Child description")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .parent(savedTask)
                .children(new ArrayList<>())
                .build();
        taskRepository.save(child);

        mockMvc.perform(delete("/api/tasks/{code}", savedTask.getCode()))
                .andExpect(status().isBadRequest());

        assertTrue(taskRepository.findByCode(savedTask.getCode()).isPresent());
    }

    // ========== GET /api/tasks/{code}/children - Get Child Tasks Tests ==========

    @Test
    @DisplayName("Should get child tasks successfully")
    void getChildTasks_WithChildren_ReturnsOk() throws Exception {
        Task child1 = Task.builder()
                .code("CH-01-child1")
                .title("Child Task 1")
                .description("First child")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .parent(savedTask)
                .children(new ArrayList<>())
                .build();

        Task child2 = Task.builder()
                .code("CH-02-child2")
                .title("Child Task 2")
                .description("Second child")
                .status(TaskStatus.IN_PROGRESS)
                .createdId(1L)
                .parent(savedTask)
                .children(new ArrayList<>())
                .build();

        taskRepository.save(child1);
        taskRepository.save(child2);

        mockMvc.perform(get("/api/tasks/{code}/children", savedTask.getCode()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("CH-01-child1"))
                .andExpect(jsonPath("$[1].code").value("CH-02-child2"));
    }

    @Test
    @DisplayName("Should return empty list when no children")
    void getChildTasks_NoChildren_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/tasks/{code}/children", savedTask.getCode()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should return 404 when parent not found")
    void getChildTasks_ParentNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/{code}/children", "NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/tasks/root - Get Root Tasks Tests ==========

    @Test
    @DisplayName("Should get root tasks successfully")
    void getRootTasks_WithRoots_ReturnsOk() throws Exception {
        Task root1 = Task.builder()
                .code("RT-01-root1")
                .title("Root Task 1")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .children(new ArrayList<>())
                .build();

        Task root2 = Task.builder()
                .code("RT-02-root2")
                .title("Root Task 2")
                .status(TaskStatus.IN_PROGRESS)
                .createdId(1L)
                .children(new ArrayList<>())
                .build();

        // Create a child task (should not appear in root list)
        Task child = Task.builder()
                .code("CH-01-child")
                .title("Child Task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .parent(root1)
                .children(new ArrayList<>())
                .build();

        taskRepository.save(root1);
        taskRepository.save(root2);
        taskRepository.save(child);

        mockMvc.perform(get("/api/tasks/root"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3)) // savedTask + root1 + root2
                .andExpect(jsonPath("$[*].code").value(hasItems("TS-01-test1", "RT-01-root1", "RT-02-root2")))
                .andExpect(jsonPath("$[*].code").value(not(hasItem("CH-01-child"))));
    }

    @Test
    @DisplayName("Should return empty list when no root tasks")
    void getRootTasks_NoRoots_ReturnsEmptyList() throws Exception {
        taskRepository.deleteAll();

        // Create only child tasks
        Task parent = Task.builder()
                .code("PR-01-parent")
                .title("Parent Task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .children(new ArrayList<>())
                .build();
        Task child = Task.builder()
                .code("CH-01-child")
                .title("Child Task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .parent(parent)
                .children(new ArrayList<>())
                .build();
        taskRepository.save(parent);
        taskRepository.save(child);

        mockMvc.perform(get("/api/tasks/root"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1)) // Only parent (root)
                .andExpect(jsonPath("$[0].code").value("PR-01-parent"));
    }

    // ========== Complex Scenarios ==========

    @Test
    @DisplayName("Should handle task hierarchy correctly")
    void taskHierarchy_ComplexScenario_WorksCorrectly() throws Exception {
        // Create a 3-level hierarchy
        Task root = Task.builder()
                .code("RT-01-root")
                .title("Root Task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .children(new ArrayList<>())
                .build();
        root = taskRepository.save(root);

        Task level2 = Task.builder()
                .code("LV-02-level2")
                .title("Level 2 Task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .parent(root)
                .children(new ArrayList<>())
                .build();
        level2 = taskRepository.save(level2);

        Task level3 = Task.builder()
                .code("LV-03-level3")
                .title("Level 3 Task")
                .status(TaskStatus.PENDING)
                .createdId(1L)
                .parent(level2)
                .children(new ArrayList<>())
                .build();
        taskRepository.save(level3);

        // Get root tasks
        mockMvc.perform(get("/api/tasks/root"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(hasItem("RT-01-root")));

        // Get children of root
        mockMvc.perform(get("/api/tasks/{code}/children", root.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("LV-02-level2"));

        // Get children of level2
        mockMvc.perform(get("/api/tasks/{code}/children", level2.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("LV-03-level3"));

        // Get level3 task should show parent
        mockMvc.perform(get("/api/tasks/{code}", level3.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentCode").value("LV-02-level2"))
                .andExpect(jsonPath("$.hierarchyLevel").value(3));
    }
}