package com.filecollection.core;

import com.filecollection.config.FileCollectionProperties;
import com.filecollection.entity.FileMetadata;
import com.filecollection.entity.OperationLog;
import com.filecollection.mapper.FileMetadataMapper;
import com.filecollection.mapper.OperationLogMapper;
import com.filecollection.strategy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionService {
    
    private final FileCollectionProperties properties;
    private final FileCopier fileCopier;
    private final TaskManager taskManager;
    private final FileMetadataMapper fileMetadataMapper;
    private final OperationLogMapper operationLogMapper;
    
    public TaskManager.TaskStatus executeCollection(List<String> upstreamNames) {
        String taskId = taskManager.createTask();
        long startTime = System.currentTimeMillis();
        
        int totalFiles = 0;
        int successCount = 0;
        int failCount = 0;
        long totalBytes = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // 获取要执行的上游列表
            List<FileCollectionProperties.UpstreamConfig> upstreams = properties.getUpstreams();
            if (upstreamNames != null && !upstreamNames.isEmpty()) {
                upstreams = upstreams.stream()
                    .filter(u -> upstreamNames.contains(u.getName()))
                    .toList();
            }
            
            // 遍历每个上游
            for (FileCollectionProperties.UpstreamConfig upstream : upstreams) {
                FileSystemStrategy sourceFs = createFileSystem(upstream);
                
                try {
                    sourceFs.connect();
                    log.info("Connected to upstream: {}", upstream.getName());
                    
                    List<String> files = sourceFs.listFiles(upstream.getPath(), upstream.getFilePattern());
                    log.info("Found {} files in upstream: {}", files.size(), upstream.getName());
                    
                    // 遍历每个文件，拷贝到所有下游
                    for (String fileName : files) {
                        String sourcePath = upstream.getPath() + "/" + fileName;
                        long fileSize = sourceFs.getFileSize(sourcePath);
                        
                        for (FileCollectionProperties.DownstreamConfig downstream : properties.getDownstreams()) {
                            String targetPath = downstream.getPath() + "/" + fileName;
                            
                            try {
                                long copyStart = System.currentTimeMillis();
                                fileCopier.copyFile(sourceFs, sourcePath, new LocalFileSystem(downstream.getPath(), "*.*"), targetPath);
                                long copyDuration = System.currentTimeMillis() - copyStart;
                                
                                // 记录文件元数据
                                FileMetadata metadata = new FileMetadata();
                                metadata.setFileName(fileName);
                                metadata.setFilePath(targetPath);
                                metadata.setFileSize(fileSize);
                                metadata.setUpstreamName(upstream.getName());
                                metadata.setCopyTime(LocalDateTime.now());
                                metadata.setStatus("SUCCESS");
                                fileMetadataMapper.insert(metadata);
                                
                                // 记录操作日志
                                OperationLog opLog = new OperationLog();
                                opLog.setTaskId(taskId);
                                opLog.setUpstreamName(upstream.getName());
                                opLog.setSourcePath(sourcePath);
                                opLog.setTargetPath(targetPath);
                                opLog.setFileSize(fileSize);
                                opLog.setCopyDuration(copyDuration);
                                opLog.setStatus("SUCCESS");
                                opLog.setCreateTime(LocalDateTime.now());
                                operationLogMapper.insert(opLog);
                                
                                successCount++;
                                totalBytes += fileSize;
                                log.debug("File copied: {} -> {} ({}ms)", sourcePath, targetPath, copyDuration);
                                
                            } catch (Exception e) {
                                failCount++;
                                errors.add(fileName + ": " + e.getMessage());
                                log.error("Failed to copy file: {}", sourcePath, e);
                                
                                // 记录失败日志
                                OperationLog opLog = new OperationLog();
                                opLog.setTaskId(taskId);
                                opLog.setUpstreamName(upstream.getName());
                                opLog.setSourcePath(sourcePath);
                                opLog.setTargetPath(targetPath);
                                opLog.setFileSize(fileSize);
                                opLog.setStatus("FAILED");
                                opLog.setErrorMessage(e.getMessage());
                                opLog.setCreateTime(LocalDateTime.now());
                                operationLogMapper.insert(opLog);
                            }
                        }
                        totalFiles++;
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to process upstream: {}", upstream.getName(), e);
                    errors.add(upstream.getName() + ": " + e.getMessage());
                } finally {
                    sourceFs.disconnect();
                }
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            taskManager.completeTask(taskId, totalFiles, successCount, failCount, totalBytes, durationMs);
            
            log.info("Collection completed: taskId={}, total={}, success={}, fail={}, duration={}ms",
                    taskId, totalFiles, successCount, failCount, durationMs);
            
        } catch (Exception e) {
            taskManager.failTask(taskId, e.getMessage());
            log.error("Collection failed", e);
        }
        
        return taskManager.getTask(taskId);
    }
    
    private FileSystemStrategy createFileSystem(FileCollectionProperties.UpstreamConfig config) {
        return switch (config.getType().toUpperCase()) {
            case "FTP" -> new FtpFileSystem(config.getHost(), config.getPort(), 
                    config.getUsername(), config.getPassword(), config.getPath());
            case "CIFS" -> new CifsFileSystem(config.getHost(), config.getShareName(),
                    config.getUsername(), config.getPassword(), config.getPath());
            case "LOCAL" -> new LocalFileSystem(config.getPath(), config.getFilePattern());
            default -> throw new IllegalArgumentException("Unknown file system type: " + config.getType());
        };
    }
}
