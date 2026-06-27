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

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionService {
    
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String ERROR_CONCURRENT_TASK = "Another collection task is already running";
    
    private final FileCollectionProperties properties;
    private final FileCopier fileCopier;
    private final TaskManager taskManager;
    private final FileMetadataMapper fileMetadataMapper;
    private final OperationLogMapper operationLogMapper;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    public TaskManager.TaskStatus executeCollection(List<String> upstreamNames) {
        if (!running.compareAndSet(false, true)) {
            log.warn("Collection task rejected - another task is already running");
            String taskId = taskManager.createTask();
            taskManager.failTask(taskId, ERROR_CONCURRENT_TASK);
            return taskManager.getTask(taskId);
        }
        
        try {
            return doExecuteCollection(upstreamNames);
        } finally {
            running.set(false);
        }
    }
    
    private TaskManager.TaskStatus doExecuteCollection(List<String> upstreamNames) {
        String taskId = taskManager.createTask();
        long startTime = System.currentTimeMillis();
        
        int totalFiles = 0;
        int successCount = 0;
        int failCount = 0;
        long totalBytes = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            List<FileCollectionProperties.UpstreamConfig> upstreams = filterUpstreams(upstreamNames);
            
            for (FileCollectionProperties.UpstreamConfig upstream : upstreams) {
                var result = processUpstream(taskId, upstream);
                totalFiles += result.totalFiles;
                successCount += result.successCount;
                failCount += result.failCount;
                totalBytes += result.totalBytes;
                errors.addAll(result.errors);
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
    
    private List<FileCollectionProperties.UpstreamConfig> filterUpstreams(List<String> upstreamNames) {
        List<FileCollectionProperties.UpstreamConfig> upstreams = properties.getUpstreams();
        if (upstreamNames != null && !upstreamNames.isEmpty()) {
            upstreams = upstreams.stream()
                .filter(u -> upstreamNames.contains(u.getName()))
                .toList();
        }
        return upstreams;
    }
    
    private UpstreamResult processUpstream(String taskId, FileCollectionProperties.UpstreamConfig upstream) {
        UpstreamResult result = new UpstreamResult();
        FileSystemStrategy sourceFs = createFileSystem(upstream);
        
        try {
            sourceFs.connect();
            log.info("Connected to upstream: {}", upstream.getName());
            
            List<String> files = sourceFs.listFiles(upstream.getPath(), upstream.getFilePattern());
            log.info("Found {} files in upstream: {}", files.size(), upstream.getName());
            
            for (String fileName : files) {
                var fileResult = processFile(taskId, upstream, sourceFs, fileName);
                result.totalFiles++;
                result.successCount += fileResult.successCount;
                result.failCount += fileResult.failCount;
                result.totalBytes += fileResult.totalBytes;
                result.errors.addAll(fileResult.errors);
            }
            
        } catch (Exception e) {
            log.error("Failed to process upstream: {}", upstream.getName(), e);
            result.errors.add(upstream.getName() + ": " + e.getMessage());
        } finally {
            sourceFs.disconnect();
        }
        
        return result;
    }
    
    private FileResult processFile(String taskId, FileCollectionProperties.UpstreamConfig upstream,
                                   FileSystemStrategy sourceFs, String fileName) {
        FileResult result = new FileResult();
        String sourcePath = buildPath(upstream.getPath(), fileName);
        long fileSize = sourceFs.getFileSize(sourcePath);
        
        for (FileCollectionProperties.DownstreamConfig downstream : properties.getDownstreams()) {
            String targetPath = buildPath(downstream.getPath(), fileName);
            
            try {
                long copyDuration = copyFileWithTiming(sourceFs, sourcePath, downstream, targetPath);
                recordSuccess(taskId, upstream.getName(), sourcePath, targetPath, fileSize, copyDuration);
                result.successCount++;
                result.totalBytes += fileSize;
                log.debug("File copied: {} -> {} ({}ms)", sourcePath, targetPath, copyDuration);
                
            } catch (Exception e) {
                result.failCount++;
                result.errors.add(fileName + ": " + e.getMessage());
                log.error("Failed to copy file: {}", sourcePath, e);
                recordFailure(taskId, upstream.getName(), sourcePath, targetPath, fileSize, e.getMessage());
            }
        }
        
        return result;
    }
    
    private long copyFileWithTiming(FileSystemStrategy sourceFs, String sourcePath,
                                    FileCollectionProperties.DownstreamConfig downstream, String targetPath) {
        long copyStart = System.currentTimeMillis();
        fileCopier.copyFile(sourceFs, sourcePath, new LocalFileSystem(downstream.getPath(), "*.*"), targetPath);
        return System.currentTimeMillis() - copyStart;
    }
    
    private void recordSuccess(String taskId, String upstreamName, String sourcePath, 
                               String targetPath, long fileSize, long copyDuration) {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(new File(targetPath).getName());
        metadata.setFilePath(targetPath);
        metadata.setFileSize(fileSize);
        metadata.setUpstreamName(upstreamName);
        metadata.setCopyTime(LocalDateTime.now());
        metadata.setStatus(STATUS_SUCCESS);
        fileMetadataMapper.insert(metadata);
        
        OperationLog opLog = createOperationLog(taskId, upstreamName, sourcePath, targetPath, fileSize);
        opLog.setCopyDuration(copyDuration);
        opLog.setStatus(STATUS_SUCCESS);
        operationLogMapper.insert(opLog);
    }
    
    private void recordFailure(String taskId, String upstreamName, String sourcePath,
                               String targetPath, long fileSize, String errorMessage) {
        OperationLog opLog = createOperationLog(taskId, upstreamName, sourcePath, targetPath, fileSize);
        opLog.setStatus(STATUS_FAILED);
        opLog.setErrorMessage(errorMessage);
        operationLogMapper.insert(opLog);
    }
    
    private OperationLog createOperationLog(String taskId, String upstreamName, String sourcePath,
                                            String targetPath, long fileSize) {
        OperationLog opLog = new OperationLog();
        opLog.setTaskId(taskId);
        opLog.setUpstreamName(upstreamName);
        opLog.setSourcePath(sourcePath);
        opLog.setTargetPath(targetPath);
        opLog.setFileSize(fileSize);
        opLog.setCreateTime(LocalDateTime.now());
        return opLog;
    }
    
    private String buildPath(String base, String fileName) {
        // 统一使用 / 作为路径分隔符，兼容 FTP/CIFS/LOCAL
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizedBase + "/" + fileName;
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
    
    @lombok.Data
    private static class UpstreamResult {
        private int totalFiles = 0;
        private int successCount = 0;
        private int failCount = 0;
        private long totalBytes = 0;
        private List<String> errors = new ArrayList<>();
    }
    
    @lombok.Data
    private static class FileResult {
        private int successCount = 0;
        private int failCount = 0;
        private long totalBytes = 0;
        private List<String> errors = new ArrayList<>();
    }
}
