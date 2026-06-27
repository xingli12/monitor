package com.filecollection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String taskId;
    private String upstreamName;
    private String sourcePath;
    private String targetPath;
    private Long fileSize;
    private Long copyDuration;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
}
