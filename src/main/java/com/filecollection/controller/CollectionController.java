package com.filecollection.controller;

import com.filecollection.config.FileCollectionProperties;
import com.filecollection.controller.dto.*;
import com.filecollection.core.CollectionService;
import com.filecollection.core.TaskManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class CollectionController {
    
    private final CollectionService collectionService;
    private final TaskManager taskManager;
    private final FileCollectionProperties properties;
    
    @PostMapping("/execute")
    public TaskResponse<ExecuteResponse> execute(@RequestBody(required = false) ExecuteRequest request) {
        try {
            var taskStatus = collectionService.executeCollection(
                request != null ? request.getUpstreamNames() : null
            );
            
            TaskSummary summary = new TaskSummary(
                taskStatus.getTotalFiles(),
                taskStatus.getSuccessCount(),
                taskStatus.getFailCount(),
                taskStatus.getTotalBytes(),
                taskStatus.getDurationMs()
            );
            
            ExecuteResponse response = new ExecuteResponse(
                taskStatus.getTaskId(),
                taskStatus.getStatus(),
                summary
            );
            
            return TaskResponse.success(response);
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
    public TaskResponse<ConfigResponse> getConfig() {
        ConfigResponse config = new ConfigResponse(
            properties.getRateLimit(),
            properties.getUpstreams().size(),
            properties.getDownstreams().size()
        );
        return TaskResponse.success(config);
    }
}
