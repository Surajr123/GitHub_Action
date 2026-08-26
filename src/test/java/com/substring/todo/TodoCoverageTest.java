package com.substring.todo;

import com.substring.todo.controllers.TodoController;
import com.substring.todo.models.Todo;
import com.substring.todo.services.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TodoCoverageTest {

    private TodoService todoService;
    private TodoController todoController;

    @BeforeEach
    void setUp() {
        todoService = new TodoService();
        todoController = new TodoController(todoService);
        todoService.deleteAllTodos();
    }

    @Test
    void service_initDummyData_shouldPopulateFourTodosOnlyOnce() {
        todoService.initDummyData();
        assertEquals(4, todoService.getAllTodos().size());

        todoService.initDummyData();
        assertEquals(4, todoService.getAllTodos().size());
    }

    @Test
    void service_createAndFindTodo_shouldGenerateIdAndCreatedFieldsAndSupportSearch() {
        Todo firstTodo = new Todo();
        firstTodo.setTitle("Spring Boot Guide");
        firstTodo.setDescription("Learn REST API development");
        firstTodo.setCompleted(false);

        Todo created = todoService.createTodo(firstTodo);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
        assertEquals("Spring Boot Guide", created.getTitle());
        assertEquals("Learn REST API development", created.getDescription());
        assertFalse(created.isCompleted());

        Todo secondTodo = new Todo("existing-id", "Docker Basics", "Manage containers", true, LocalDateTime.now(), LocalDateTime.now());
        Todo createdSecond = todoService.createTodo(secondTodo);

        assertEquals("existing-id", createdSecond.getId());
        assertEquals(2, todoService.getAllTodos().size());

        Optional<Todo> found = todoService.getTodoById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Spring Boot Guide", found.get().getTitle());

        assertTrue(todoService.getTodoById("missing-id").isEmpty());

        List<Todo> byTitle = todoService.searchTodos("spring");
        assertEquals(1, byTitle.size());
        assertEquals("Spring Boot Guide", byTitle.get(0).getTitle());

        List<Todo> byDescription = todoService.searchTodos("containers");
        assertEquals(1, byDescription.size());
        assertEquals("Docker Basics", byDescription.get(0).getTitle());

        List<Todo> noMatch = todoService.searchTodos("not-found");
        assertTrue(noMatch.isEmpty());

        List<Todo> emptyQuery = todoService.searchTodos("   ");
        assertEquals(2, emptyQuery.size());
    }

    @Test
    void service_updateTodo_shouldReplaceValuesAndKeepPreviousOnNulls() {
        Todo original = todoService.createTodo(new Todo("todo-1", "Old title", "Old description", false, LocalDateTime.now(), LocalDateTime.now()));

        Todo updateWithValues = new Todo();
        updateWithValues.setTitle("New title");
        updateWithValues.setDescription("New description");
        updateWithValues.setCompleted(true);

        Optional<Todo> updated = todoService.updateTodo(original.getId(), updateWithValues);

        assertTrue(updated.isPresent());
        assertEquals("New title", updated.get().getTitle());
        assertEquals("New description", updated.get().getDescription());
        assertTrue(updated.get().isCompleted());
        assertNotNull(updated.get().getUpdatedAt());

        Todo updateWithNulls = new Todo();
        updateWithNulls.setCompleted(false);

        Optional<Todo> updatedAgain = todoService.updateTodo(original.getId(), updateWithNulls);

        assertTrue(updatedAgain.isPresent());
        assertEquals("New title", updatedAgain.get().getTitle());
        assertEquals("New description", updatedAgain.get().getDescription());
        assertFalse(updatedAgain.get().isCompleted());

        assertTrue(todoService.updateTodo("missing-id", updateWithValues).isEmpty());
    }

    @Test
    void service_deleteTodo_and_deleteAllTodos_shouldWorkAcrossCases() {
        Todo todo = todoService.createTodo(new Todo("task-123", "Delete me", "Delete this item", false, LocalDateTime.now(), LocalDateTime.now()));

        assertTrue(todoService.deleteTodo("TASK-123"));
        assertTrue(todoService.getTodoById("task-123").isEmpty());
        assertFalse(todoService.deleteTodo("missing-id"));

        Todo anotherTodo = todoService.createTodo(new Todo("task-456", "Keep me", "Keep this item", true, LocalDateTime.now(), LocalDateTime.now()));
        assertEquals(1, todoService.getAllTodos().size());

        todoService.deleteAllTodos();
        assertTrue(todoService.getAllTodos().isEmpty());
        assertFalse(todoService.deleteTodo("task-456"));
    }

    @Test
    void controller_shouldHandleCreateReadSearchUpdateAndDeleteFlows() {
        Todo createPayload = new Todo();
        createPayload.setTitle("Write tests");
        createPayload.setDescription("Achieve full coverage");
        createPayload.setCompleted(false);

        ResponseEntity<Todo> createResponse = todoController.createTodo(createPayload);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertNotNull(createResponse.getBody().getId());

        String todoId = createResponse.getBody().getId();

        ResponseEntity<List<Todo>> allTodosResponse = todoController.getAllTodos();
        assertEquals(HttpStatus.OK, allTodosResponse.getStatusCode());
        assertEquals(1, allTodosResponse.getBody().size());

        ResponseEntity<List<Todo>> emptySearchResponse = todoController.searchTodos("   ");
        assertEquals(HttpStatus.OK, emptySearchResponse.getStatusCode());
        assertEquals(1, emptySearchResponse.getBody().size());

        ResponseEntity<List<Todo>> textSearchResponse = todoController.searchTodos("coverage");
        assertEquals(HttpStatus.OK, textSearchResponse.getStatusCode());
        assertEquals(1, textSearchResponse.getBody().size());

        ResponseEntity<Todo> byIdResponse = todoController.getTodoById(todoId);
        assertEquals(HttpStatus.OK, byIdResponse.getStatusCode());
        assertEquals("Write tests", byIdResponse.getBody().getTitle());

        ResponseEntity<Todo> missingResponse = todoController.getTodoById("missing-id");
        assertEquals(HttpStatus.NOT_FOUND, missingResponse.getStatusCode());

        Todo updatePayload = new Todo();
        updatePayload.setTitle("Updated title");
        updatePayload.setDescription("Updated description");
        updatePayload.setCompleted(true);

        ResponseEntity<Todo> updateResponse = todoController.updateTodo(todoId, updatePayload);
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Updated title", updateResponse.getBody().getTitle());
        assertEquals("Updated description", updateResponse.getBody().getDescription());
        assertTrue(updateResponse.getBody().isCompleted());

        ResponseEntity<Todo> missingUpdateResponse = todoController.updateTodo("missing-id", updatePayload);
        assertEquals(HttpStatus.NOT_FOUND, missingUpdateResponse.getStatusCode());

        ResponseEntity<Void> deleteResponse = todoController.deleteTodo(todoId);
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        ResponseEntity<Void> missingDeleteResponse = todoController.deleteTodo("missing-id");
        assertEquals(HttpStatus.NOT_FOUND, missingDeleteResponse.getStatusCode());
    }
}
