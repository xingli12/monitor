package com.filecollection.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskSummary {
    private int totalFiles;
    private int successCount;
    private int failCount;
    private long totalBytes;
    private long durationMs;
}
