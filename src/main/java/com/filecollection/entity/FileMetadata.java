package com.filecollection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_metadata")
public class FileMetadata {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String md5Hash;
    private String upstreamName;
    private LocalDateTime copyTime;
    private String status;
}
