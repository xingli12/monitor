package com.filecollection.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteResponse {
    private String taskId;
    private String status;
    private TaskSummary summary;
}
