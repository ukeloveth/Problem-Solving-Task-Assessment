./mvnw clean install## Base URL: `/api/tasks`

#### 1. Create Task
- **POST** `/api/tasks`
- **Request Body:**

  {
  "title": "Task Title",
  "description": "Task description",
  "status": "PENDING",
  "assignedDate": "2024-01-15T10:00:00",
  "dueDate": "2024-01-20T17:00:00",
  "creatorId": 1,
  "assigneeId": 2,
  "parentCode": "AB-12-xyz1",  // Optional
  "priority": "HIGH",
  "tags": "tag1,tag2"
  }
    - **Response:** 201 Created with TaskResponseDTO

#### 2. Get Task by Code
- **GET** `/api/tasks/{code}`
- **Response:** 200 OK with TaskResponseDTO

#### 3. Get All Tasks (Paginated)
- **GET** `/api/tasks?page=0&size=10&sortBy=createAt&sortDir=desc`
- **Query Parameters:**
    - `page`: Page number (default: 0)
    - `size`: Page size (default: 10)
    - `sortBy`: Field to sort by (default: createAt)
    - `sortDir`: Sort direction - asc/desc (default: desc)
- **Response:** 200 OK with Page<TaskResponseDTO>

#### 4. Update Task
- **PUT** `/api/tasks/{code}`
- **Request Body:** Same as Create Task
- **Response:** 200 OK with updated TaskResponseDTO

#### 5. Delete Task
- **DELETE** `/api/tasks/{code}`
- **Response:** 204 No Content
- **Note:** Cannot delete tasks that have children

#### 6. Get Child Tasks
- **GET** `/api/tasks/{code}/children`
- **Response:** 200 OK with List<TaskResponseDTO>

#### 7. Get Root Tasks
- **GET** `/api/tasks/root`
- **Response:** 200 OK with List<TaskResponseDTO>

## 📝 Task Model

### Task Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Auto-generated unique identifier |
| `code` | String | Unique task code (format: AA-NN-xxxx) |
| `title` | String | Task title (required) |
| `description` | String | Task description |
| `status` | TaskStatus | Task status: PENDING, IN_PROGRESS (required) |
| `assignedDate` | LocalDateTime | Date when task was assigned |
| `dueDate` | LocalDateTime | Task due date |
| `creatorId` | Long | ID of user who created the task (required) |
| `assigneeId` | Long | ID of user assigned to the task |
| `parentCode` | String | Code of parent task (optional) |
| `priority` | String | Task priority level |
| `tags` | String | Comma-separated tags |
| `createdAt` | LocalDateTime | Auto-generated creation timestamp |
| `updatedAt` | LocalDateTime | Auto-generated last update timestamp |
| `hierarchyLevel` | int | Calculated hierarchy level (1-5) |
| `childCodes` | List<String> | List of child task codes |

### Task Status Enum

- `PENDING`
- `IN_PROGRESS`

## 🔑 Unique Code Generation

Tasks are automatically assigned unique codes in the format: `AA-NN-xxxx`

- **AA**: 2 uppercase letters (e.g., AB, CD)
- **NN**: 2 digits (e.g., 12, 34)
- **xxxx**: 4 alphanumeric characters (digits and lowercase letters)

**Example:** `AB-12-xyz1`, `CD-34-abcd`

## 🌳 Task Hierarchy

- Tasks can have parent-child relationships
- Maximum hierarchy depth: **5 levels**
- Root tasks have no parent
- Tasks with children cannot be deleted
- Circular references are prevented during updates

### Hierarchy Rules

1. Creating a task with a parent at level 5 will fail
2. Updating a task's parent cannot create circular references
3. Deleting a task requires all children to be deleted first

## ✅ Validation

### Required Fields

- `title`: Must not be blank
- `status`: Must not be null
- `creatorId`: Must not be null

### Business Rules

- Task codes must be unique
- Maximum hierarchy level is 5
- Cannot delete tasks with children
- Cannot set circular parent references

## 🧪 Testing

### Run All Testsash
./mvnw test### Test Coverage

- **Unit Tests**: `TaskServiceImplTest` - Tests service layer logic
- **Integration Tests**: `TaskControllerIntegrationTest` - Tests full HTTP layer with database

### Test Database

Tests use H2 in-memory database that is automatically reset between tests.

## 📊 Error Responses

All errors follow a consistent format:

{
"timestamp": "2024-01-15T10:00:00",
"status": 400,
"error": "Validation Failed",
"message": "Error message here",
"errors": {
"fieldName": "Field error message"
}
}### HTTP Status Codes

- **200 OK**: Successful GET/PUT request
- **201 Created**: Successful POST request
- **204 No Content**: Successful DELETE request
- **400 Bad Request**: Validation error or business rule violation
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Unexpected server error

## ⚙️ Configuration

### Application Properties

Located in `src/main/resources/application.yml`:

spring:
application:
name: task-management-api
datasource:
url: jdbc:h2:mem:taskdb
jpa:
hibernate:
ddl-auto: update
show-sql: true

server:
port: 8080### Customization

To change the database or port, modify `application.yml`. For production, configure a persistent database (PostgreSQL, MySQL, etc.).

## 🛠️ Development

### Building for Production

./mvnw clean packageThe JAR file will be created in `target/` directory.

### Running Production Build

java -jar target/test-0.0.1-SNAPSHOT.jar## 📚 Key Classes

- **TaskController**: REST API endpoints
- **TaskService/TaskServiceImpl**: Business logic and transaction management
- **TaskRepository**: Data access layer with custom queries
- **Task**: JPA entity with bidirectional parent-child relationship
- **UniqueCodeGenerator**: Generates unique task codes
- **GlobalExceptionHandler**: Centralized exception handling

## 🔐 Security Considerations

Currently, the API has CORS enabled for all origins (`@CrossOrigin(origins = "*")`). For production:

- Implement authentication/authorization
- Restrict CORS to specific domains
- Use HTTPS
- Add rate limiting
- Implement API key or JWT authentication

## 📝 License

This project is licensed under the MIT License (or specify your license).

## 👥 Contributors

- ukeloveth247@gmail.com

## 📞 Support

For issues or questions, please open an issue in the repository.

---

**Happy Task Managing! 🎯**