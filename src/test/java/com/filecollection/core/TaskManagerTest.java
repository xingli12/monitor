package com.filecollection.core;

import com.filecollection.config.FileCollectionProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagerTest {

    private TaskManager createManager() {
        return new TaskManager(new FileCollectionProperties());
    }

    @Test
    void shouldCreateAndGetTask() {
        // Given
        TaskManager manager = createManager();

        // When
        String taskId = manager.createTask();
        TaskManager.TaskStatus task = manager.getTask(taskId);

        // Then
        assertNotNull(taskId);
        assertNotNull(task);
        assertEquals("RUNNING", task.getStatus());
        assertNotNull(task.getStartTime());
    }

    @Test
    void shouldReturnNullForNonExistentTask() {
        // Given
        TaskManager manager = createManager();

        // When
        TaskManager.TaskStatus task = manager.getTask("non-existent-id");

        // Then
        assertNull(task);
    }

    @Test
    void shouldCompleteTaskSuccessfully() {
        // Given
        TaskManager manager = createManager();
        String taskId = manager.createTask();

        // When
        manager.completeTask(taskId, 100, 95, 5, 1024000, 5000);
        TaskManager.TaskStatus task = manager.getTask(taskId);

        // Then
        assertEquals("SUCCESS", task.getStatus());
        assertNotNull(task.getEndTime());
        assertEquals(100, task.getTotalFiles());
        assertEquals(95, task.getSuccessCount());
        assertEquals(5, task.getFailCount());
        assertEquals(1024000, task.getTotalBytes());
        assertEquals(5000, task.getDurationMs());
    }

    @Test
    void shouldFailTaskWithError() {
        // Given
        TaskManager manager = createManager();
        String taskId = manager.createTask();

        // When
        manager.failTask(taskId, "Connection timeout");
        TaskManager.TaskStatus task = manager.getTask(taskId);

        // Then
        assertEquals("FAILED", task.getStatus());
        assertNotNull(task.getEndTime());
        assertEquals("Connection timeout", task.getError());
    }

    @Test
    void shouldNotFailNonExistentTask() {
        // Given
        TaskManager manager = createManager();

        // When & Then
        assertDoesNotThrow(() -> manager.failTask("non-existent", "error"));
    }

    @Test
    void shouldNotCompleteNonExistentTask() {
        // Given
        TaskManager manager = createManager();

        // When & Then
        assertDoesNotThrow(() -> manager.completeTask("non-existent", 0, 0, 0, 0, 0));
    }

    @Test
    void shouldManageMultipleTasksIndependently() {
        // Given
        TaskManager manager = createManager();
        String task1Id = manager.createTask();
        String task2Id = manager.createTask();

        // When
        manager.completeTask(task1Id, 50, 50, 0, 500000, 2000);
        manager.failTask(task2Id, "Network error");

        // Then
        TaskManager.TaskStatus task1 = manager.getTask(task1Id);
        TaskManager.TaskStatus task2 = manager.getTask(task2Id);

        assertEquals("SUCCESS", task1.getStatus());
        assertEquals(50, task1.getTotalFiles());

        assertEquals("FAILED", task2.getStatus());
        assertEquals("Network error", task2.getError());
    }

    @Test
    void shouldGenerateUniqueTaskIds() {
        // Given
        TaskManager manager = createManager();

        // When
        String id1 = manager.createTask();
        String id2 = manager.createTask();
        String id3 = manager.createTask();

        // Then
        assertNotEquals(id1, id2);
        assertNotEquals(id2, id3);
        assertNotEquals(id1, id3);
    }

    @Test
    void shouldSetCorrectTimestamps() {
        // Given
        TaskManager manager = createManager();
        String taskId = manager.createTask();

        // When
        TaskManager.TaskStatus task = manager.getTask(taskId);

        // Then
        assertNotNull(task.getStartTime());
        assertNull(task.getEndTime());
    }

    @Test
    void shouldCleanupExpiredRunningTasks() {
        // Given
        TaskManager manager = createManager();
        String taskId = manager.createTask();
        TaskManager.TaskStatus task = manager.getTask(taskId);

        // Manually backdate the task's start time to 3 hours ago
        task.setStartTime(java.time.LocalDateTime.now().minusHours(3));

        // When
        manager.cleanupExpiredTasks();

        // Then
        assertNull(manager.getTask(taskId));
    }
}
