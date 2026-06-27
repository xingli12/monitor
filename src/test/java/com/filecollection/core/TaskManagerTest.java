package com.filecollection.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagerTest {
    
    @Test
    void shouldCreateAndGetTask() {
        // Given
        TaskManager manager = new TaskManager();
        
        // When
        String taskId = manager.createTask();
        TaskManager.TaskStatus task = manager.getTask(taskId);
        
        // Then
        assertNotNull(taskId);
        assertNotNull(task);
        assertEquals("RUNNING", task.getStatus());
    }
}
