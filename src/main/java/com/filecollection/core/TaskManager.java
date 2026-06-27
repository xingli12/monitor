package com.filecollection.core;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskManager {
    
    private final Map<String, TaskStatus> tasks = new ConcurrentHashMap<>();
    
    public String createTask() {
        String taskId = UUID.randomUUID().toString();
        TaskStatus status = new TaskStatus();
        status.setTaskId(taskId);
        status.setStatus("RUNNING");
        status.setStartTime(LocalDateTime.now());
        tasks.put(taskId, status);
        return taskId;
    }
    
    public TaskStatus getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    public void completeTask(String taskId, int totalFiles, int successCount, int failCount, long totalBytes, long durationMs) {
        TaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("SUCCESS");
            task.setEndTime(LocalDateTime.now());
            task.setTotalFiles(totalFiles);
            task.setSuccessCount(successCount);
            task.setFailCount(failCount);
            task.setTotalBytes(totalBytes);
            task.setDurationMs(durationMs);
        }
    }
    
    public void failTask(String taskId, String error) {
        TaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("FAILED");
            task.setEndTime(LocalDateTime.now());
            task.setError(error);
        }
    }
    
    @Data
    public static class TaskStatus {
        private String taskId;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalFiles;
        private int successCount;
        private int failCount;
        private long totalBytes;
        private long durationMs;
        private String error;
    }
}
