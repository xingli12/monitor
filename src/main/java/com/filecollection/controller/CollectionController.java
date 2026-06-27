package com.filecollection.controller;

import com.filecollection.config.FileCollectionProperties;
import com.filecollection.controller.dto.ExecuteRequest;
import com.filecollection.controller.dto.TaskResponse;
import com.filecollection.core.CollectionService;
import com.filecollection.core.TaskManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class CollectionController {
    
    private final CollectionService collectionService;
    private final TaskManager taskManager;
    private final FileCollectionProperties properties;
    
    @PostMapping("/execute")
    public TaskResponse<Map<String, Object>> execute(@RequestBody(required = false) ExecuteRequest request) {
        try {
            var taskStatus = collectionService.executeCollection(
                request != null ? request.getUpstreamNames() : null
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskStatus.getTaskId());
            data.put("status", taskStatus.getStatus());
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalFiles", taskStatus.getTotalFiles());
            summary.put("successCount", taskStatus.getSuccessCount());
            summary.put("failCount", taskStatus.getFailCount());
            summary.put("totalBytes", taskStatus.getTotalBytes());
            summary.put("durationMs", taskStatus.getDurationMs());
            data.put("summary", summary);
            
            return TaskResponse.success(data);
        } catch (Exception e) {
            return TaskResponse.error(e.getMessage());
        }
    }
    
    @GetMapping("/tasks/{taskId}")
    public TaskResponse<TaskManager.TaskStatus> getTask(@PathVariable String taskId) {
        TaskManager.TaskStatus task = taskManager.getTask(taskId);
        if (task == null) {
            return TaskResponse.error("Task not found: " + taskId);
        }
        return TaskResponse.success(task);
    }
    
    @GetMapping("/config")
    public TaskResponse<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimit", properties.getRateLimit());
        config.put("upstreamCount", properties.getUpstreams().size());
        config.put("downstreamCount", properties.getDownstreams().size());
        return TaskResponse.success(config);
    }
}
