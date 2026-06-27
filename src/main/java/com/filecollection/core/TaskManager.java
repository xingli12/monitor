package com.filecollection.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TaskManager {
    
    private static final long TASK_EXPIRY_MINUTES = 60;
    
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
    
    @Scheduled(fixedRate = 300000) // 每5分钟清理一次
    public void cleanupExpiredTasks() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(TASK_EXPIRY_MINUTES);
        Iterator<Map.Entry<String, TaskStatus>> iterator = tasks.entrySet().iterator();
        int removedCount = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<String, TaskStatus> entry = iterator.next();
            TaskStatus task = entry.getValue();
            
            if (task.getEndTime() != null && task.getEndTime().isBefore(expiryTime)) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.info("Cleaned up {} expired tasks, remaining: {}", removedCount, tasks.size());
        }
    }
    
    public int getActiveTaskCount() {
        return (int) tasks.values().stream()
            .filter(t -> "RUNNING".equals(t.getStatus()))
            .count();
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
